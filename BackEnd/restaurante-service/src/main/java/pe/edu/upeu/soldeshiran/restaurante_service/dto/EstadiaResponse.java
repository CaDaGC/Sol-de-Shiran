package pe.edu.upeu.soldeshiran.restaurante_service.dto;

public record EstadiaResponse(
        Long idReserva,
        String codigoReserva,
        Long idCabana,
        String nombreCabana,
        String nombreHuesped
) {
}