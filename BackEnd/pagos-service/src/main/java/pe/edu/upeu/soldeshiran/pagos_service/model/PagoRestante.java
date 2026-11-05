package pe.edu.upeu.soldeshiran.pagos_service.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="pago_restante") @Getter @Setter @NoArgsConstructor
public class PagoRestante {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true) private Long idReserva;
 @Column(nullable=false, unique=true, length=36) private String claveIdempotencia;
 @Column(nullable=false) private Long idTrabajador;
 @Column(nullable=false, precision=10, scale=2) private BigDecimal monto;
 @Column(nullable=false, length=30) private String metodoPago;
 @Column(length=100) private String numeroOperacion;
 @Column(nullable=false) private boolean completado;
 @Column(nullable=false) private LocalDateTime fechaRegistro=LocalDateTime.now();
}
