package pe.edu.upeu.soldeshiran.usuario_service.service;

import java.io.IOException;
import java.util.UUID;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
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
  @Value("${usuarios.fotos.directorio:uploads/perfiles}")
  private String directorio;

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
      var foto = usuario;
      Path carpeta=Paths.get(directorio).toAbsolutePath().normalize();
      Files.createDirectories(carpeta);
      String nombre=UUID.randomUUID() + (formato.equals("png") ? ".png" : ".jpg");
      Path destino=carpeta.resolve(nombre);
      Files.write(destino, archivo.getBytes(), StandardOpenOption.CREATE_NEW);
      foto.setFotoPerfil("/api/v1/usuarios/fotos/" + nombre);
      try {
        return repository.saveAndFlush(foto);
      } catch (RuntimeException e) {
        Files.deleteIfExists(destino);
        throw e;
      }
    } catch (IOException | IllegalArgumentException ex) {
      throw error("El archivo no contiene una foto válida.");
    }
  }

  public Usuario obtener(Actor actor) {
    return repository.findById(actor.id()).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada."));
  }

  public void quitar(Actor actor) {
    var usuario = obtener(actor);
    usuario.setFotoPerfil(null);
    repository.save(usuario);
  }
  private ResponseStatusException error(String mensaje) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
  }
}