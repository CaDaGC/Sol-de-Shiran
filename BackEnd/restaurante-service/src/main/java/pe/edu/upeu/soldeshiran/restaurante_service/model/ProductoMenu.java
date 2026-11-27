package pe.edu.upeu.soldeshiran.restaurante_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "producto_menu",
        indexes = {
                @Index(
                        name = "idx_producto_restaurante_categoria",
                        columnList = "id_restaurante,id_categoria"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductoMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaMenu categoria;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private Restaurante restaurante;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull
    @Min(0)
    @Column(name = "tiempo_preparacion_min", nullable = false)
    private Integer tiempoPreparacionMin = 20;

    @NotNull
    @Column(nullable = false)
    private Boolean disponible = true;

    @Size(max = 500)
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @PrePersist
    @PreUpdate
    private void validarCategoriaRestaurante() {
        if (categoria == null || restaurante == null) {
            throw new IllegalStateException(
                    "El producto necesita una categoría y un restaurante."
            );
        }

        var restauranteCategoria = categoria.getRestaurante();

        if (restauranteCategoria == null
                || restaurante.getIdRestaurante() == null
                || !Objects.equals(
                restaurante.getIdRestaurante(),
                restauranteCategoria.getIdRestaurante()
        )) {
            throw new IllegalStateException(
                    "La categoría del producto debe pertenecer al mismo restaurante."
            );
        }
    }
}