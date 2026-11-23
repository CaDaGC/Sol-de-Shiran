package pe.edu.upeu.soldeshiran.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistroResponse(Long id, String nombre, String email, String rol) {}
