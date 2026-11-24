package pe.edu.upeu.soldeshiran.reservas_service.dto;

import java.util.List;

public record MisReservasPaginaResponse(
        List<MiReservaResponse> content, int page, int size, long totalElements, int totalPages) {
}
