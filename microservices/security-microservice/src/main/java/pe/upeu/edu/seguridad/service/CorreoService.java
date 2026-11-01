package pe.upeu.edu.seguridad.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender sender;
    private final String from;

    public CorreoService(JavaMailSender sender, @Value("${app.mail-from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void recuperar(String email, String token) {
        var mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject("Recuperar acceso a Sol de Shirán");
        mail.setText(
                "Tu c�digo de recuperaci�n de Sol de Shir�n es:\n\n"
                        + token
                        + "\n\nC�pialo en la opci�n de restablecer contrase�a. Vence en 15 minutos"
                        + " y puede usarse una sola vez."
                        + "\n\nSi no lo solicitaste, ignora este correo.");
        sender.send(mail);
    }
}
