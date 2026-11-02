package pe.edu.upeu.soldeshiran.reservas_service.dto;

import java.time.LocalDate;
import java.util.List;

public record FechasOcupadasResponse(
        Long idCabana,
        LocalDate desde,
        LocalDate hasta,
        List<LocalDate> fechasOcupadas
) {
}