package pe.edu.upeu.soldeshiran.reservas_service.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BloqueoFecha {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long idCabana;
  private LocalDate desde;
  private LocalDate hasta;
  private String motivo;
  private boolean activo = true;
}
