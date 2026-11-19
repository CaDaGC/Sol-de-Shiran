package pe.edu.upeu.soldeshiran.catalogo_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.FotoCabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;
import pe.edu.upeu.soldeshiran.catalogo_service.service.FotoCabanaService;

class FotoCabanaServiceTest {
  private final FotoCabanaRepository repository = mock(FotoCabanaRepository.class);
  private final FotoCabanaService service = new FotoCabanaService(repository);

  @Test void guardaPngRealYConservaContenido() throws Exception {
    var salida = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", salida);
    var bytes = salida.toByteArray();
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    var foto = service.guardar(new MockMultipartFile("archivo", "foto.png", "image/png", bytes), new Actor(1L,"ADMIN"));
    assertEquals("image/png", foto.getTipo());
    assertArrayEquals(bytes, foto.getContenido());
    assertNotNull(java.util.UUID.fromString(foto.getId()));
  }

  @Test void rechazaArchivoDisfrazadoDeImagen() {
    var archivo = new MockMultipartFile("archivo", "foto.png", "image/png", "<script>bad</script>".getBytes());
    assertEquals(400, assertThrows(ResponseStatusException.class,
        () -> service.guardar(archivo, new Actor(1L,"ADMIN"))).getStatusCode().value());
    verifyNoInteractions(repository);
  }

  @Test void rechazaCargaPorCliente() {
    var archivo = new MockMultipartFile("archivo", new byte[]{1});
    assertEquals(403, assertThrows(ResponseStatusException.class,
        () -> service.guardar(archivo, new Actor(2L,"CLIENTE"))).getStatusCode().value());
    verifyNoInteractions(repository);
  }

  @Test void rechazaArchivosGrandes() {
    var archivo = new MockMultipartFile("archivo", new byte[2 * 1024 * 1024 + 1]);
    assertEquals(400, assertThrows(ResponseStatusException.class,
        () -> service.guardar(archivo, new Actor(1L,"ADMIN"))).getStatusCode().value());
    verifyNoInteractions(repository);
  }
}