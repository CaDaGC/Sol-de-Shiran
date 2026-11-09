package pe.edu.upeu.soldeshiran.auth_service.dto;

import lombok.Data;

// BLABLA BLA XDDD
@Data
public class LoginRequest {
  @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
  private String email;

  @jakarta.validation.constraints.NotBlank private String password;
}
