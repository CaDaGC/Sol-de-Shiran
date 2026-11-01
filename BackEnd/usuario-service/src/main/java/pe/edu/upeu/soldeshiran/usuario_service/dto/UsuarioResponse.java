package pe.edu.upeu.soldeshiran.usuario_service.dto;

import lombok.Data;

@Data
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String rol;

}
