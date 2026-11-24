package pe.edu.upeu.soldeshiran.reservas_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReservaFiltroRequest(
        @Size(max = 100) String buscar,
        @Positive Long idCabana,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size) {
    public ReservaFiltroRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
