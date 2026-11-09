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
    private final pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient catalogo;
    @org.springframework.beans.factory.annotation.Value("${servicios.interno.token}") private String token;

    @Transactional(readOnly=true)
    public pe.edu.upeu.soldeshiran.reservas_service.dto.SaldoCheckInResponse saldoCheckIn(Long id) {
        Reserva r = reservaRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe la reserva."));
        validarCheckIn(r);
        return new pe.edu.upeu.soldeshiran.reservas_service.dto.SaldoCheckInResponse(id,r.getSaldoPendiente(),r.getEstadoReserva().name());
    }
    private void validarCheckIn(Reserva r) {
        java.time.LocalDate hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        if (r.getEstadoReserva() != EstadoReserva.CONFIRMADA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede realizar el check-in de una reserva confirmada.");
        var formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (hoy.isBefore(r.getFechaIngreso()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El check-in estará disponible desde el " + r.getFechaIngreso().format(formato) + ". Todavía no se ha registrado el pago restante.");
        if (!hoy.isBefore(r.getFechaSalida()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La estancia terminó el " + r.getFechaSalida().format(formato) + ". No se puede iniciar el check-in.");
    }
    @Transactional
    public void checkIn(Long id, pe.edu.upeu.soldeshiran.reservas_service.dto.CheckInRequest request) {
        Reserva r=reservaRepository.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe la reserva."));
        if(request.referenciaPago().equals(r.getReferenciaPagoRestante())) return;
        validarCheckIn(r);
        if(r.getSaldoPendiente().compareTo(request.monto())!=0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El importe no coincide con el saldo pendiente.");
        try { catalogo.ocupar(r.getIdCabana(),id,token); }
        catch(feign.FeignException e) {
            if (e.status() == 401 || e.status() == 403)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Catálogo rechazó la credencial interna. Configura el mismo INTERNAL_SERVICE_TOKEN en catálogo, reservas y pagos. El pago registrado se conserva.");
            throw new ResponseStatusException(e.status()==409?HttpStatus.CONFLICT:HttpStatus.SERVICE_UNAVAILABLE,"No se pudo confirmar la ocupación de la cabaña.");
        }
        r.setSaldoPendiente(java.math.BigDecimal.ZERO);
        r.setTotalPagado(r.getMontoTotal());
        r.setEstadoReserva(EstadoReserva.OCUPADA);
        r.setReferenciaPagoRestante(request.referenciaPago());
        reservaRepository.saveAndFlush(r);
    }


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
    public void iniciarAdelanto(Long idReserva, Long idUsuario) {
        Reserva reserva = reservaRepository.bloquearPorId(idReserva).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reserva."));
        if (!idUsuario.equals(reserva.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva pertenece a otro usuario.");
        }
        if (reserva.getEstadoReserva() != EstadoReserva.PENDIENTE_PAGO
                || !reserva.getExpiraEn().isAfter(LocalDateTime.now())
                || (reserva.getTotalPagado() != null && reserva.getTotalPagado().signum() > 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La reserva no admite un adelanto en línea.");
        }
        reserva.setAdelantoEnLinea(true);
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

        reserva.setTotalPagado(request.montoAprobado());
        reserva.setSaldoPendiente(reserva.getMontoTotal().subtract(request.montoAprobado()));
        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);
    }
}