package pe.edu.upeu.soldeshiran.restaurante_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.IdentidadUsuarioResponse;

@FeignClient(name = "USUARIO-SERVICE")
public interface UsuariosClient {

    @GetMapping("/internal/usuarios/{id}")
    IdentidadUsuarioResponse identidad(
            @PathVariable("id") Long id,
            @RequestHeader("X-Internal-Key") String clave
    );
}