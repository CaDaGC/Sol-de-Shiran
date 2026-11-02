package pe.edu.upeu.soldeshiran.reservas_service.service;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservaExpirationScheduler {

    private final ReservaRepository reservaRepository;

    @Scheduled(fixedDelayString = "${reservas.limpieza-cada-ms:60000}")
    @Transactional
    public void expirarReservasVencidas() {
        reservaRepository.expirarPendientesVencidas(
                EstadoReserva.PENDIENTE_PAGO,
                EstadoReserva.EXPIRADA,
                LocalDateTime.now()
        );
    }
}