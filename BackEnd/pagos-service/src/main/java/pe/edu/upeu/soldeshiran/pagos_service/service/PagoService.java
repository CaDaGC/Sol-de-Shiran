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

    PagoResponse consultarEstado(Long idUsuario, Long idTransaccion);
    void procesarNotificacion(String orderId);
}
