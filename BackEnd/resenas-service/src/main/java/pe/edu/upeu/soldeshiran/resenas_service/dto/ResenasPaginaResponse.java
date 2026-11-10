package pe.edu.upeu.soldeshiran.resenas_service.dto;

import java.util.List;

public record ResenasPaginaResponse(List<ResenaResponse> opiniones, boolean hayMas) {}
