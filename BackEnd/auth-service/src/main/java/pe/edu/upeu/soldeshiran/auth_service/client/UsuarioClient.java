package pe.edu.upeu.soldeshiran.auth_service.client;

import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usuario-service")
public interface UsuarioClient {

    @GetMapping("/api/v1/usuarios/email/{email}")
    UsuarioResponse findByEmail(@PathVariable String email);
}
