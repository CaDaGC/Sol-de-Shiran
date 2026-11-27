package pe.edu.upeu.soldeshiran.reservas_service.dto;

public record EstadiaRestauranteResponse(
        Long idReserva,
        String codigoReserva,
        Long idCabana,
        String nombreCabana,
        String nombreHuesped
) {
}