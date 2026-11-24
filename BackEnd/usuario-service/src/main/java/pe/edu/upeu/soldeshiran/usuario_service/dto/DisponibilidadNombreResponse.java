package pe.edu.upeu.soldeshiran.usuario_service.dto;

import java.util.List;

public record DisponibilidadNombreResponse(
    String nombre, boolean disponible, List<String> sugerencias) {}
