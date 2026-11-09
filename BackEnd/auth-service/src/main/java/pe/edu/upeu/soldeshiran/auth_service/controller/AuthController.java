package pe.edu.upeu.soldeshiran.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.auth_service.dto.AuthResponse;
import pe.edu.upeu.soldeshiran.auth_service.dto.LoginRequest;
import pe.edu.upeu.soldeshiran.auth_service.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public AuthResponse login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
