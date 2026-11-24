package pe.edu.upeu.soldeshiran.usuario_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "persona")
@Getter @Setter
public class Persona {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_persona") private Long id;
  @Column(nullable = false, length = 100) private String nombres;
  @Column(nullable = false, length = 100) private String apellidos;
  @Column(name = "tipo_documento", nullable = false, length = 20)
  private String tipoDocumento = "DNI";
  @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
  private String numeroDocumento;
  @Column(nullable = false, length = 20) private String telefono;
  @Column(length = 255) private String direccion;
  @Column(name = "fecha_creacion", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime fechaCreacion;
  @Column(name = "fecha_actualizacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
  private LocalDateTime fechaActualizacion;

  @PrePersist void crear() {
    fechaCreacion = LocalDateTime.now();
    fechaActualizacion = fechaCreacion;
  }
  @PreUpdate void actualizar() { fechaActualizacion = LocalDateTime.now(); }
}
