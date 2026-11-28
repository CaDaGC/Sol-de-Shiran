package pe.edu.upeu.soldeshiran.restaurante_service.service;

import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface FotoMenuService {
    String guardar(String tipo, Long id, MultipartFile archivo, Actor actor);
    void quitar(String tipo, Long id, Actor actor);
    byte[] leer(String tipo, Long id);
}
