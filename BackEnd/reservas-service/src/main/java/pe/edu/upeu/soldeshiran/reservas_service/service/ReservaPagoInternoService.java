package pe.edu.upeu.soldeshiran.reservas_service.service;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ConfirmarPagoInternoRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaPagoInternoResponse;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservaPagoInternoService {
    private final ReservaRepository reservaRepository;

    @Transactional(readOnly = true)
    public ReservaPagoInternoResponse consultar(Long idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la reserva."
                ));

        return new ReservaPagoInternoResponse(
                reserva.getIdReserva(),
                reserva.getIdUsuario(),
                reserva.getMontoAdelanto(),
                reserva.getEstadoReserva().name(),
                reserva.getExpiraEn()
        );
    }

    @Transactional
    public void confirmar(Long idReserva, ConfirmarPagoInternoRequest request) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la reserva."
                ));

        if (reserva.getEstadoReserva() == EstadoReserva.CONFIRMADA) {
            return;
        }

        if (reserva.getEstadoReserva() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva no está pendiente de pago."
            );
        }

        if (reserva.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva venció antes de ser confirmada."
            );
        }

        if (reserva.getMontoAdelanto().compareTo(request.montoAprobado()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El importe aprobado no coincide con el adelanto de la reserva."
            );
        }

        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);
    }
}