package pe.edu.upeu.soldeshiran.auth_service.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private Long userId;
    private String nombre;
    private String email;
    private String rol;
}