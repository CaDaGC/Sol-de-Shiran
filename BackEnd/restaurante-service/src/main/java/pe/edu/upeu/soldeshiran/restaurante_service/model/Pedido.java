package pe.edu.upeu.soldeshiran.restaurante_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "pedido",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pedido_usuario_solicitud",
                        columnNames = {"id_usuario", "clave_solicitud"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_pedido_usuario_fecha",
                        columnList = "id_usuario,fecha_pedido"
                ),
                @Index(
                        name = "idx_pedido_restaurante_estado",
                        columnList = "id_restaurante,estado_pedido"
                ),
                @Index(
                        name = "idx_pedido_reserva_estado",
                        columnList = "id_reserva,estado_pedido"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Version
    private Long version;

    @Column(
            name = "codigo_pedido",
            nullable = false,
            unique = true,
            length = 20
    )
    private String codigoPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private Restaurante restaurante;

    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_usuario_restaurante")
    private Long idUsuarioRestaurante;

    @Column(name = "nombre_cabana", nullable = false, length = 100)
    private String nombreCabana;

    @Column(name = "nombre_huesped", nullable = false, length = 150)
    private String nombreHuesped;

    @Column(
            name = "monto_total",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 30)
    private EstadoPedido estadoPedido = EstadoPedido.NUEVO;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "metodo_pago_presencial",
            nullable = false,
            length = 30
    )
    private MetodoPagoPresencial metodoPagoPresencial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPagoPedido estadoPago = EstadoPagoPedido.PENDIENTE;

    @Column(name = "fecha_pedido", nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(
            name = "clave_solicitud",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String claveSolicitud;

    @Column(
            name = "huella_solicitud",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String huellaSolicitud;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("idDetalle ASC")
    private List<DetallePedido> detalles = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }
}