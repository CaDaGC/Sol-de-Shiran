package pe.edu.upeu.soldeshiran.usuario_service.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(@NotNull Boolean activo) {}
