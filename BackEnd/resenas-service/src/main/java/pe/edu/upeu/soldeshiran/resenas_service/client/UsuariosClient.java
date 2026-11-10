package pe.edu.upeu.soldeshiran.resenas_service.client;

import java.util.List;
import pe.edu.upeu.soldeshiran.resenas_service.dto.AutorResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.IdentidadUsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USUARIO-SERVICE")
public interface UsuariosClient {
    @GetMapping("/internal/usuarios/{id}")
    IdentidadUsuarioResponse identidad(@PathVariable("id") Long id, @RequestHeader("X-Internal-Key") String clave);

    @GetMapping("/internal/usuarios/resumen")
    List<AutorResenaResponse> autores(@RequestParam("ids") List<Long> ids, @RequestHeader("X-Internal-Key") String clave);

}
