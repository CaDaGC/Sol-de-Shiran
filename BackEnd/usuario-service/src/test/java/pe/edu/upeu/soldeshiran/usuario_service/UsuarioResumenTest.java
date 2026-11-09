package pe.edu.upeu.soldeshiran.usuario_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.edu.upeu.soldeshiran.usuario_service.controller.UsuarioResumenController;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.config.import="})
class UsuarioResumenTest {
    @Autowired UsuarioRepository repository;

    @Test
    void buscaNombreOCorreoSinDistinguirMayusculasYEscapaComodines() {
        guardar("Antonella", "antonella@gmail.com");
        guardar("Cliente 100%", "otro_1@gmail.com");
        var controller = new UsuarioResumenController(repository);
        assertThat(controller.consultar(" ANTONELLA ", null)).hasSize(1);
        assertThat(controller.consultar("OTRO_1@", null)).hasSize(1);
        assertThat(controller.consultar("%", null)).hasSize(1);
        assertThat(controller.consultar("_", null)).hasSize(1);
        assertThat(controller.consultar("inexistente", null)).isEmpty();
    }

    @Test
    void consultaUnLoteSinIncluirOtrosUsuarios() {
        Usuario usuario = guardar("Antonella", "antonella@gmail.com");
        guardar("Otro", "otro@gmail.com");
        var resultado = repository.resumirPorIds(List.of(usuario.getId()));
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().nombre()).isEqualTo("Antonella");
    }

    private Usuario guardar(String nombre, String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword("hash-no-incluido-en-resumen");
        return repository.saveAndFlush(usuario);
    }
}
