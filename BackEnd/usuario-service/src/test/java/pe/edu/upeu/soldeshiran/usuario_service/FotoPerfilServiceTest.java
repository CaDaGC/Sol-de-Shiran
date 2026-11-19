package pe.edu.upeu.soldeshiran.usuario_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;
import pe.edu.upeu.soldeshiran.usuario_service.service.FotoPerfilService;

class FotoPerfilServiceTest {
  final UsuarioRepository repository = mock(UsuarioRepository.class);
  final FotoPerfilService service = new FotoPerfilService(repository);
  final Actor cliente = new Actor(7L, "CLIENTE");
  Usuario cuenta() {
    var u = new Usuario(); u.setId(7L);
    when(repository.findById(7L)).thenReturn(Optional.of(u));
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    return u;
  }

  @Test void clienteGuardaFotoSoloEnSuPropiaCuenta() throws Exception {
    var u = cuenta();
    var salida = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", salida);
    var bytes = salida.toByteArray();
    service.guardar(new MockMultipartFile("archivo", "foto.png", "image/png", bytes), cliente);
    assertArrayEquals(bytes, u.getFotoPerfil());
    assertEquals("image/png", u.getTipoFotoPerfil());
    verify(repository).findById(7L);
    verify(repository).save(u);
    verifyNoMoreInteractions(repository);
  }

  @Test void quitarEliminaSoloLaFotoDeLaCuentaActual() {
    var u = cuenta(); u.setFotoPerfil(new byte[]{1}); u.setTipoFotoPerfil("image/png");
    service.quitar(cliente);
    assertNull(u.getFotoPerfil()); assertNull(u.getTipoFotoPerfil());
    assertEquals(7L, u.getId());
    verify(repository).save(u);
  }

  @Test void rechazaContenidoQueNoEsImagen() {
    cuenta();
    var archivo = new MockMultipartFile("archivo", "foto.png", "image/png", "<svg>invalid</svg>".getBytes());
    assertEquals(400, assertThrows(ResponseStatusException.class,
        () -> service.guardar(archivo, cliente)).getStatusCode().value());
    verify(repository, never()).save(any());
  }

  @Test void rechazaFotoMayorDeDosMb() {
    cuenta();
    var archivo = new MockMultipartFile("archivo", new byte[2 * 1024 * 1024 + 1]);
    assertEquals(400, assertThrows(ResponseStatusException.class,
        () -> service.guardar(archivo, cliente)).getStatusCode().value());
    verify(repository, never()).save(any());
  }
}