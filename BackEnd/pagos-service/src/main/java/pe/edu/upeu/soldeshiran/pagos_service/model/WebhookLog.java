package pe.edu.upeu.soldeshiran.pagos_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_log")
@Getter
@Setter
@NoArgsConstructor
public class WebhookLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;

    @Column(name = "id_transaccion_pasarela", length = 100)
    private String idTransaccionPasarela;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "estado_procesamiento", length = 30)
    private String estadoProcesamiento = "PROCESADO_EXITO";

    @Column(name = "fecha_recepcion", insertable = false, updatable = false)
    private LocalDateTime fechaRecepcion;
}