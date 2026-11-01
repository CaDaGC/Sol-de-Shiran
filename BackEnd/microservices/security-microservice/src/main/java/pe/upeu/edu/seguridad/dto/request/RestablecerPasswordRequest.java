package pe.upeu.edu.seguridad.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.*;

public record RestablecerPasswordRequest(
        @JsonAlias("token") @NotBlank @Size(max = 128) String codigo,
        @NotBlank @Size(min = 12, max = 72) String password) {}
