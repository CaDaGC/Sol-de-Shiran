package pe.edu.upeu.soldeshiran.reservas_service.service;

import pe.edu.upeu.soldeshiran.reservas_service.dto.CotizacionReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaResponse;

import java.time.LocalDate;

public interface ReservaService {

    CotizacionReservaResponse consultarDisponibilidad(
            Long idCabana,
            LocalDate fechaIngreso,
            LocalDate fechaSalida,
            Integer cantidadHuespedes
    );

    ReservaResponse crearReservaTemporal(
            Long idUsuario,
            CrearReservaRequest request
    );
}