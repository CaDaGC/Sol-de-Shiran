package pe.edu.upeu.soldeshiran.usuario_service.service;

import java.io.IOException;
import java.util.UUID;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService;
import pe.edu.upeu.soldeshiran.almacenamiento.CategoriaArchivo;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {
  private final UsuarioRepository repository;
  private final AlmacenamientoService almacenamiento;

  @Transactional
  public Usuario guardar(Long id, MultipartFile archivo, Actor actor) {
    if (!actor.es("ADMIN") && !actor.id().equals(id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes actualizar tu foto.");
    var usuario = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return guardarArchivo(archivo, usuario);
  }

  @Transactional
  public Usuario guardar(MultipartFile archivo, Actor actor) {
    return guardar(actor.id(), archivo, actor);
  }

  private Usuario guardarArchivo(MultipartFile archivo, Usuario usuario) {
    if (archivo.isEmpty() || archivo.getSize() > 2 * 1024 * 1024)
      throw error("Selecciona una foto de hasta 2 MB.");
    try (var entrada = ImageIO.createImageInputStream(archivo.getInputStream())) {
      var lectores = ImageIO.getImageReaders(entrada);
      if (!lectores.hasNext()) throw error("La foto debe ser un JPG o PNG válido.");
      var lector = lectores.next();
      String formato;
      try {
        lector.setInput(entrada);
        formato = lector.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!formato.equals("png") && !formato.equals("jpeg"))
          throw error("Solo se admiten fotos JPG o PNG.");
        if ((long) lector.getWidth(0) * lector.getHeight(0) > 16000000)
          throw error("La foto no debe superar 16 megapíxeles.");
        if (lector.read(0) == null) throw error("No se pudo leer la foto.");
      } finally { lector.dispose(); }
      String anterior = referenciaActual(usuario);
      var foto = usuario;
      String nombre = UUID.randomUUID() + (formato.equals("png") ? ".png" : ".jpg");
      String referencia = almacenamiento.guardar(CategoriaArchivo.PERFIL, nombre, archivo.getBytes(), formato.equals("png") ? "image/png" : "image/jpeg");
      foto.setFotoPerfil("/api/v1/usuarios/fotos/" + nombre);
      foto.setFotoPerfilArchivo(referencia);
      try {
        var guardado = repository.saveAndFlush(foto);
        retirarDespuesDeGuardar(anterior);
        return guardado;
      }
      catch (RuntimeException e) { almacenamiento.descartar(CategoriaArchivo.PERFIL, referencia); throw e; }
    } catch (IOException | IllegalArgumentException ex) {
      throw error("El archivo no contiene una foto válida.");
    }
  }

  public Usuario obtener(Actor actor) {
    return repository.findById(actor.id()).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada."));
  }

  @Transactional
  public void quitar(Actor actor) {
    var usuario = obtener(actor);
    String anterior = referenciaActual(usuario);
    usuario.setFotoPerfil(null);
    usuario.setFotoPerfilArchivo(null);
    repository.saveAndFlush(usuario);
    retirarDespuesDeGuardar(anterior);
  }
  private String referenciaActual(Usuario usuario) {
    if (usuario.getFotoPerfilArchivo() != null) return usuario.getFotoPerfilArchivo();
    String url = usuario.getFotoPerfil();
    if (url == null || !url.startsWith("/api/v1/usuarios/fotos/")) return null;
    return url.substring(url.lastIndexOf('/') + 1);
  }
  private void retirarDespuesDeGuardar(String referencia) {
    if (referencia == null) return;
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { almacenamiento.descartar(CategoriaArchivo.PERFIL, referencia); }
      });
    } else almacenamiento.descartar(CategoriaArchivo.PERFIL, referencia);
  }
  public byte[] leerPublica(String nombre) {
    if (!nombre.matches("(?:[a-f0-9-]{36}|migrada-[0-9]+)\\.(png|jpg)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    var usuario = repository.findByFotoPerfil("/api/v1/usuarios/fotos/" + nombre);
    String referencia = usuario.map(Usuario::getFotoPerfilArchivo).orElse(nombre);
    return almacenamiento.leer(CategoriaArchivo.PERFIL, referencia);
  }
  @Transactional
  public boolean migrar(Long id) {
    var usuario = repository.findById(id).orElseThrow();
    if (usuario.getFotoPerfil() == null || (usuario.getFotoPerfilArchivo() != null && usuario.getFotoPerfilArchivo().startsWith("drive:"))) return false;
    String nombre = usuario.getFotoPerfil().substring(usuario.getFotoPerfil().lastIndexOf('/') + 1);
    byte[] contenido = almacenamiento.leer(CategoriaArchivo.PERFIL, usuario.getFotoPerfilArchivo() == null ? nombre : usuario.getFotoPerfilArchivo());
    String referencia = almacenamiento.migrar(CategoriaArchivo.PERFIL, nombre, contenido, nombre.endsWith(".png") ? "image/png" : "image/jpeg");
    usuario.setFotoPerfilArchivo(referencia);
    repository.saveAndFlush(usuario);
    return true;
  }
  private ResponseStatusException error(String mensaje) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
  }
}