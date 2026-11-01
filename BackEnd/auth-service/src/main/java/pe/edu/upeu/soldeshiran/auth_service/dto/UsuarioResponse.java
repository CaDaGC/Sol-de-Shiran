package pe.edu.upeu.soldeshiran.auth_service.dto;

import lombok.Data;

@Data
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String rol;
}
