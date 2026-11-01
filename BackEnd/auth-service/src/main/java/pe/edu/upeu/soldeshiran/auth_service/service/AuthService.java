package pe.edu.upeu.soldeshiran.auth_service.service;

import pe.edu.upeu.soldeshiran.auth_service.dto.AuthResponse;
import pe.edu.upeu.soldeshiran.auth_service.dto.LoginRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
