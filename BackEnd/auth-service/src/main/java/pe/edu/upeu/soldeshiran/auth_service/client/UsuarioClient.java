package pe.edu.upeu.soldeshiran.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;

@FeignClient(name = "usuario-service")
public interface UsuarioClient {

  @GetMapping("/internal/usuarios/autenticacion")
  UsuarioResponse findByEmail(
      @org.springframework.web.bind.annotation.RequestParam("email") String email,
      @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String key);
}
