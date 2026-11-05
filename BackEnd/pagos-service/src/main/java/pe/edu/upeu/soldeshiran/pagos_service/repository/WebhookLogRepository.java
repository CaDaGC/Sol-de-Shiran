package pe.edu.upeu.soldeshiran.pagos_service.repository;

import pe.edu.upeu.soldeshiran.pagos_service.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
}