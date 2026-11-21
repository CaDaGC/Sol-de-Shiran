package pe.edu.upeu.soldeshiran.usuario_service.dto;

import lombok.Data;

@Data
public class UsuarioRequest {

  @com.fasterxml.jackson.annotation.JsonAlias("nombre_usuario")
  private String nombreUsuario;
  private String nombre;
  private PersonaRequest persona;

  public String getNombre() {
    return nombreUsuario != null ? nombreUsuario : nombre;
  }
  private String email;
  private String password;
  private String rol;
}
