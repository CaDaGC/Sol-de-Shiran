package pe.edu.upeu.soldeshiran.reservas_service.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CrearReservaRequest(
@NotNull
        @Positive
        Long idCabana,

        @NotNull
        @FutureOrPresent
        LocalDate fechaIngreso,

        @NotNull
        LocalDate fechaSalida,

        @NotNull
        @Min(1)
        Integer cantidadHuespedes,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida
) {
  public CrearReservaRequest(
        
        Long idCabana,

        
        
        LocalDate fechaIngreso,

        
        LocalDate fechaSalida,

        
        
        Integer cantidadHuespedes) {
    this(idCabana, fechaIngreso, fechaSalida, cantidadHuespedes, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

}