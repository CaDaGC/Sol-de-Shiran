package pe.edu.upeu.soldeshiran.reservas_service.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReservasPaginaResponse(
        List<ReservaPanelResponse> content, int page, int size, long totalElements, int totalPages) {
    public static ReservasPaginaResponse from(Page<ReservaPanelResponse> pagina) {
        return new ReservasPaginaResponse(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
