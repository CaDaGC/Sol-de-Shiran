package pe.upeu.edu.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class CorreoServiceTest {
    @Test
    void enviaCodigoAlCorreoRegistradoSinEnlace() {
        var sender = mock(JavaMailSender.class);
        var correo = new CorreoService(sender, "remitente@gmail.com");
        correo.recuperar("cliente@gmail.com", "0123456789abcdef0123456789abcdef");
        var mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("cliente@gmail.com");
        assertThat(mensaje.getValue().getFrom()).isEqualTo("remitente@gmail.com");
        assertThat(mensaje.getValue().getText())
                .contains("0123456789abcdef0123456789abcdef", "15 minutos", "una sola vez")
                .doesNotContain("http");
    }
}
