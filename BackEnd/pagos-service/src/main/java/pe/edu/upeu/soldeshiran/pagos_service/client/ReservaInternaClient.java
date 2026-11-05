package pe.edu.upeu.soldeshiran.pagos_service.client;

import pe.edu.upeu.soldeshiran.pagos_service.dto.ConfirmarPagoRequest;
import pe.edu.upeu.soldeshiran.pagos_service.dto.ReservaPagoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "MS-RESERVAS")
public interface ReservaInternaClient {
    @GetMapping("/api/v1/reservas/interno/{idReserva}/saldo-check-in")
    pe.edu.upeu.soldeshiran.pagos_service.dto.SaldoCheckInResponse saldoCheckIn(@PathVariable Long idReserva,
        @RequestHeader("X-Internal-Service-Token") String token);
    @PostMapping("/api/v1/reservas/interno/{idReserva}/check-in")
    void checkIn(@PathVariable Long idReserva, @RequestHeader("X-Internal-Service-Token") String token,
        @RequestBody pe.edu.upeu.soldeshiran.pagos_service.dto.CheckInRequest request);

    @GetMapping("/api/v1/reservas/interno/{idReserva}")
    ReservaPagoResponse obtenerReserva(
            @PathVariable Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String token
    );

    @PostMapping("/api/v1/reservas/interno/{idReserva}/confirmar-pago")
    void confirmarPago(
            @PathVariable Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String token,
            @RequestBody ConfirmarPagoRequest request
    );
}