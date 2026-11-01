package pe.edu.upeu.soldeshiran.usuario_service.service;

import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioRequest;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse register(UsuarioRequest request);

    UsuarioResponse createAdmin(UsuarioRequest request);

    UsuarioResponse findByEmail(String email);

    UsuarioResponse findById(Long id);

    List<UsuarioResponse> findAll();

    UsuarioResponse update(Long id, UsuarioRequest request);

    UsuarioResponse updatePassword(Long id, String password);

}
