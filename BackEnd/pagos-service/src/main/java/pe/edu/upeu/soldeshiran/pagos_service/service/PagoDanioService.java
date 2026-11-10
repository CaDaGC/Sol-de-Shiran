package pe.edu.upeu.soldeshiran.pagos_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;

@Service
@RequiredArgsConstructor
public class PagoDanioService {
    private final TransaccionPagoRepository pagos;

    public record Cobro(@NotNull @Positive Long idTrabajador,
            @NotBlank @Size(max = 500) String descripcion,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
            @NotBlank @Pattern(regexp = "EFECTIVO|TARJETA|YAPE|PLIN") String metodoPago,
            @NotNull LocalDateTime fecha) {}

    public void registrar(Long idReserva, Cobro cobro) {
        var existente = pagos.findByReservaPagoDanos(idReserva);
        if (existente.isPresent()) {
            verificar(existente.get(), cobro);
            return;
        }

        var pago = new TransaccionPago();
        pago.setIdReserva(idReserva);
        pago.setReservaPagoDanos(idReserva);
        pago.setTipoPago(TransaccionPago.DANOS);
        pago.setIdTrabajador(cobro.idTrabajador());
        pago.setDescripcionDanos(cobro.descripcion());
        pago.setMonto(cobro.monto());
        pago.setMetodoPago(cobro.metodoPago());
        pago.setFechaTransaccion(cobro.fecha());
        pago.setEstadoTransaccion(EstadoTransaccion.APROBADA);
        try {
            pagos.saveAndFlush(pago);
        } catch (DataIntegrityViolationException e) {
            verificar(pagos.findByReservaPagoDanos(idReserva).orElseThrow(() -> e), cobro);
        }
    }

    private void verificar(TransaccionPago pago, Cobro cobro) {
        if (!Objects.equals(pago.getIdTrabajador(), cobro.idTrabajador())
                || !Objects.equals(pago.getDescripcionDanos(), cobro.descripcion())
                || pago.getMonto().compareTo(cobro.monto()) != 0
                || !Objects.equals(pago.getMetodoPago(), cobro.metodoPago())
                || !Objects.equals(pago.getFechaTransaccion(), cobro.fecha())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un pago por daños con otros datos para esta reserva.");
        }
    }
}
