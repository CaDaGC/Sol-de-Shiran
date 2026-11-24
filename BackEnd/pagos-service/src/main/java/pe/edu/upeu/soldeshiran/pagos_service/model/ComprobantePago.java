package pe.edu.upeu.soldeshiran.pagos_service.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="comprobante_pago") @Getter @Setter @NoArgsConstructor
public class ComprobantePago {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,unique=true) private Long idTransaccion;
  @Column(nullable=false,unique=true) private Long idReserva;
  @Column(nullable=false) private Long idUsuario;
  @Column(nullable=false,unique=true,length=40) private String numero;
  @Column(nullable=false) private java.time.LocalDateTime fechaEmision;
  @Column(nullable=false,length=200) private String archivo;
  @Column(nullable=false,columnDefinition="LONGTEXT") private String datosJson;
}
