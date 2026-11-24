package pe.edu.upeu.soldeshiran.reservas_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Embeddable
public record RegistroCheckout(
    @Column(name = "checkout_fecha") LocalDateTime fecha,
    @Column(name = "checkout_registrado_por") Long registradoPor,
    @Column(name = "checkout_descripcion_danos", length = 500) String descripcionDanos,
    @Column(name = "checkout_monto_danos", precision = 10, scale = 2) BigDecimal montoDanos,
    @Column(name = "checkout_medio_pago", length = 20) String medioPago) {}
