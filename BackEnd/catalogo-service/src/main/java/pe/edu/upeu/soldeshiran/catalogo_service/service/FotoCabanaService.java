package pe.edu.upeu.soldeshiran.catalogo_service.service;

import java.io.IOException;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService;
import pe.edu.upeu.soldeshiran.almacenamiento.CategoriaArchivo;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.model.FotoCabana;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.FotoCabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;

@Service
@RequiredArgsConstructor
public class FotoCabanaService {
  private final FotoCabanaRepository repository;
  private final AlmacenamientoService almacenamiento;

  @org.springframework.transaction.annotation.Transactional
  public FotoCabana guardar(MultipartFile archivo, Actor actor) {
    actor.exigir("ADMIN");
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
      var foto = new FotoCabana();
      foto.setId(UUID.randomUUID().toString());
      foto.setTipo(formato.equals("png") ? "image/png" : "image/jpeg");
      String referencia = almacenamiento.guardar(CategoriaArchivo.CABANA, foto.getId() + (formato.equals("png") ? ".png" : ".jpg"), archivo.getBytes(), foto.getTipo());
      foto.setArchivo(referencia);
      foto.setContenido(new byte[0]);
      try { return repository.saveAndFlush(foto); }
      catch (RuntimeException e) { almacenamiento.descartar(CategoriaArchivo.CABANA, referencia); throw e; }
    } catch (IOException | IllegalArgumentException ex) {
      throw error("El archivo no contiene una foto válida.");
    }
  }

  public FotoCabana obtener(String id) {
    return repository.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no encontrada."));
  }

  public byte[] contenido(FotoCabana foto) {
    return foto.getArchivo() == null ? foto.getContenido() : almacenamiento.leer(CategoriaArchivo.CABANA, foto.getArchivo());
  }
  @org.springframework.transaction.annotation.Transactional
  public boolean migrar(String id) {
    var foto = obtener(id);
    if (foto.getArchivo() != null && foto.getArchivo().startsWith("drive:")) return false;
    String referencia = almacenamiento.migrar(CategoriaArchivo.CABANA, foto.getId() + (foto.getTipo().equals("image/png") ? ".png" : ".jpg"), contenido(foto), foto.getTipo());
    foto.setArchivo(referencia);
      foto.setContenido(new byte[0]);
    foto.setContenido(new byte[0]);
    repository.saveAndFlush(foto);
    return true;
  }
  private ResponseStatusException error(String mensaje) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
  }
}