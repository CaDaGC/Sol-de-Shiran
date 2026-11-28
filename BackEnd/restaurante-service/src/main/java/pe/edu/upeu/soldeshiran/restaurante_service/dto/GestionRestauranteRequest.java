package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;

public final class GestionRestauranteRequest {

    private GestionRestauranteRequest() {
    }

    public record RestauranteDatos(
            @NotBlank(message = "Ingresa el nombre del restaurante")
            @Size(max = 100)
            String nombre,

            @Size(max = 150)
            String especialidad,

            @Size(max = 500)
            String imagenUrl
    ) {
    }

    public record EstadoDatos(
            @NotNull(message = "Selecciona el estado")
            EstadoRestaurante estado
    ) {
    }

    public record CategoriaDatos(
            @NotBlank(message = "Ingresa el nombre de la categoría")
            @Size(max = 60)
            String nombre,

            @NotNull(message = "Selecciona COMIDA o BEBIDA")
            TipoCategoria tipo
    ) {
    }

    public record ProductoDatos(
            @NotNull
            @Positive
            Long idCategoria,

            @NotBlank(message = "Ingresa el nombre del producto")
            @Size(max = 100)
            String nombre,

            @Size(max = 5000)
            String descripcion,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 8, fraction = 2)
            BigDecimal precio,

            @NotNull
            @Min(0)
            Integer tiempoPreparacionMin,

            @NotNull
            Boolean disponible,

            @Size(max = 500)
            String imagenUrl
    ) {
    }

    public record DisponibilidadDatos(
            @NotNull(message = "Indica la disponibilidad")
            Boolean disponible
    ) {
    }

    public record AsignacionDatos(
            @NotNull
            @Positive
            Long idUsuario
    ) {
    }
}