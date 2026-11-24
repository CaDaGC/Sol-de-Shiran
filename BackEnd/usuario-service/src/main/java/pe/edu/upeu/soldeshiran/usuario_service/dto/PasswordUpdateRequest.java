package pe.edu.upeu.soldeshiran.usuario_service.dto;

import lombok.Data;

@Data
public class PasswordUpdateRequest {

  private String password;
  private String passwordActual;
}
