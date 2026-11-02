package pe.edu.upeu.soldeshiran.catalogo_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cabana_imagenes")
@Getter
@Setter
@NoArgsConstructor
public class CabanaImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long idImagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cabana", nullable = false)
    private Cabana cabana;

    @Column(name = "url_imagen", nullable = false, length = 255)
    private String urlImagen;
}