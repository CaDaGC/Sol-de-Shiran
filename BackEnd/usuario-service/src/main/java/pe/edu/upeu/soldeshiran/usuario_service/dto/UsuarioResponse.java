package pe.edu.upeu.soldeshiran.usuario_service.dto;

import lombok.Data;

@Data
public class UsuarioResponse {

  private Long id;
  private String nombre;
  private String nombreUsuario;
  private String fotoPerfil;
  private PersonaResponse persona;
  private String email;
  private Boolean activo;
  private String rol;
  private java.time.LocalDateTime fechaCreacion;
}
