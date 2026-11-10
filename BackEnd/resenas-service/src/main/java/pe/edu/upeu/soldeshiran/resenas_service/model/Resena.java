package pe.edu.upeu.soldeshiran.resenas_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "resena",
       uniqueConstraints = @UniqueConstraint(name = "uq_reserva_resena", columnNames = "id_reserva"),
       indexes = @Index(name = "idx_resena_cabana", columnList = "id_cabana"))
@Check(constraints = "puntuacion BETWEEN 1 AND 5")
public class Resena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long idResena;

    @Column(name = "id_cabana", nullable = false)
    private Long idCabana;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    @Column(nullable = false)
    private int puntuacion;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_creacion", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCreacion;

    public Resena(Long cabana, Long usuario, Long reserva, int puntuacion, String comentario) {
        this.idCabana = cabana;
        this.idUsuario = usuario;
        this.idReserva = reserva;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fechaCreacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }
}
