package pe.edu.upeu.soldeshiran.pagos_service.service;

import pe.edu.upeu.soldeshiran.pagos_service.dto.*;

public interface PagoService {
    PagoResponse procesarPagoBrick(
            Long idUsuario,
            Long idReserva,
            CrearPagoBrickRequest request
    );

    PreferenciaResponse crearPreferencia(
            Long idUsuario,
            Long idReserva,
            CrearPreferenciaRequest request
    );

    void procesarNotificacion(String paymentId);
}