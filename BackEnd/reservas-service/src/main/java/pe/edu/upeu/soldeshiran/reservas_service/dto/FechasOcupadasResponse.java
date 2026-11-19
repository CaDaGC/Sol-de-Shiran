package pe.edu.upeu.soldeshiran.reservas_service.dto;

import java.time.LocalDate;
import java.util.List;

public record FechasOcupadasResponse(
        Long idCabana,
        LocalDate desde,
        LocalDate hasta,
        List<LocalDate> fechasOcupadas, List<Intervalo> intervalos
) {
    public record Intervalo(java.time.LocalDateTime inicio, java.time.LocalDateTime fin) {}
    public FechasOcupadasResponse(Long id, LocalDate desde, LocalDate hasta, List<LocalDate> fechas) {
        this(id, desde, hasta, fechas, List.of());
    }
}