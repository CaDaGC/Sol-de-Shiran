package pe.edu.upeu.soldeshiran.usuario_service.controller;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.usuario_service.mapper.UsuarioMapper;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;
import pe.edu.upeu.soldeshiran.usuario_service.service.FotoPerfilService;

@RestController @RequiredArgsConstructor
@RequestMapping({"/api/v1/usuarios", "/api/usuarios"})
public class FotoUsuarioController {
  private final FotoPerfilService fotos;
  private final UsuarioMapper mapper;
  @PostMapping(value="/{id}/foto", consumes="multipart/form-data")
  public UsuarioResponse guardar(@PathVariable Long id, @RequestParam("file") MultipartFile file,
      HttpServletRequest request) {
    return mapper.toResponse(fotos.guardar(id,file,Actor.desde(request)));
  }
}
