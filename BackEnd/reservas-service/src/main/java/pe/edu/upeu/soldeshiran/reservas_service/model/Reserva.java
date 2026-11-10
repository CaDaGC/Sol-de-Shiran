package pe.edu.upeu.soldeshiran.reservas_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "reserva",
    indexes = {
      @Index(
          name = "idx_reserva_cabana_fechas",
          columnList = "id_cabana,fecha_ingreso,fecha_salida"),
      @Index(name = "idx_reserva_estado_expira", columnList = "estado_reserva,expira_en")
    })
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_reserva")
  private Long idReserva;

  @Version private Long version;

  @Embedded
  private RegistroCheckout checkout;

  @Column(name = "checkout_pago_danos_exportado")
  private Boolean checkoutPagoDanosExportado;

  @Column(columnDefinition = "boolean default false")
  private Boolean adelantoEnLinea = false;

  @Column(precision = 10, scale = 2, columnDefinition = "decimal(10,2) default 0.00")
  private java.math.BigDecimal totalPagado = java.math.BigDecimal.ZERO;

    @Column(name="referencia_pago_restante", unique=true, length=100)
    private String referenciaPagoRestante;

    @Column(name = "id_cabana", nullable = false)
    private Long idCabana;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Column(name = "fecha_ingreso", nullable = false)
  private LocalDate fechaIngreso;

  @Column(name = "fecha_salida", nullable = false)
  private LocalDate fechaSalida;

  @Column(name = "cantidad_huespedes", nullable = false)
  private Integer cantidadHuespedes;

  @Column(name = "tarifa_noche_aplicada", nullable = false, precision = 10, scale = 2)
  private BigDecimal tarifaNocheAplicada;

  @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoTotal;

  @Column(name = "monto_adelanto", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoAdelanto;

  @Column(name = "saldo_pendiente", nullable = false, precision = 10, scale = 2)
  private BigDecimal saldoPendiente;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_reserva", nullable = false, length = 30)
  private EstadoReserva estadoReserva = EstadoReserva.PENDIENTE_PAGO;

  @Column(name = "expira_en", nullable = false)
  private LocalDateTime expiraEn;

  @Column(name = "fecha_creacion", insertable = false, updatable = false)
  private LocalDateTime fechaCreacion;
}
