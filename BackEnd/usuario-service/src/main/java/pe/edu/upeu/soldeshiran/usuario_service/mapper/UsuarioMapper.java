package pe.edu.upeu.soldeshiran.usuario_service.mapper;

import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioRequest;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRequest request) {
        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        return usuario;
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();

        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setPassword(usuario.getPassword());
        response.setRol(usuario.getRol().getNombre());
        return response;
    }

}
