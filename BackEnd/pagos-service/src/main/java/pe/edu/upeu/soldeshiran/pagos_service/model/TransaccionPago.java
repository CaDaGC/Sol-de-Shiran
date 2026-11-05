package pe.edu.upeu.soldeshiran.pagos_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion_pago")
@Getter
@Setter
@NoArgsConstructor
public class TransaccionPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    @Column(name = "tipo_pago", nullable = false, length = 30)
    private String tipoPago = "ONLINE_ADELANTO";

    @Column(name = "id_externo_pasarela", unique = true, length = 100)
    private String idExternoPasarela;

    @Column(name = "solicitud_checkout", columnDefinition = "TEXT")
    private String solicitudCheckout;

    @Column(name = "numero_operacion_fisica", length = 50)
    private String numeroOperacionFisica;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(length = 10)
    private String moneda = "PEN";

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago = "MERCADO_PAGO_BRICK";

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transaccion", nullable = false, length = 30)
    private EstadoTransaccion estadoTransaccion = EstadoTransaccion.PENDIENTE;

    @Column(name = "fecha_transaccion", insertable = false, updatable = false)
    private LocalDateTime fechaTransaccion;
}