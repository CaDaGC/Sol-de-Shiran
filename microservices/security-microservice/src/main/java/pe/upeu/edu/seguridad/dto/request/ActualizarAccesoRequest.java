package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

import pe.upeu.edu.seguridad.enums.Rol;

public record ActualizarAccesoRequest(@NotNull Rol rol, @NotNull Boolean activo) {}
