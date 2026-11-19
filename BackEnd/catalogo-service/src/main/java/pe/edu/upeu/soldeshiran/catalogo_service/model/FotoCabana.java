package pe.edu.upeu.soldeshiran.catalogo_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fotos_cabana")
@Getter @Setter @NoArgsConstructor
public class FotoCabana {
  @Id private String id;
  @Column(nullable = false) private String tipo;
  @Lob @Column(nullable = false, columnDefinition = "LONGBLOB") private byte[] contenido;
}