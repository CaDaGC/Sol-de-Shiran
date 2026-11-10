package pe.edu.upeu.soldeshiran.resenas_service.dto;

import java.time.LocalDateTime;

public record ResenaResponse(Long id, String autor, int puntuacion, String comentario, LocalDateTime fecha) {}
