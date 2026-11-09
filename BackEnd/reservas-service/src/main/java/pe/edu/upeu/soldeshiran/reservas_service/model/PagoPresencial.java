package pe.edu.upeu.soldeshiran.reservas_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "pago_presencial",
    uniqueConstraints = @UniqueConstraint(columnNames = "clave_operacion"))
public class PagoPresencial {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long reservaId;
  private Long registradoPor;

  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal monto;

  private String medio;
  private String referencia;

  @Column(name = "clave_operacion", nullable = false, length = 80)
  private String claveOperacion;

  private LocalDateTime fecha = LocalDateTime.now();
}
