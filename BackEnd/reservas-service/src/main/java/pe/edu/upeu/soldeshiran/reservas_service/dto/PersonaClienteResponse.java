package pe.edu.upeu.soldeshiran.reservas_service.dto;
public record PersonaClienteResponse(Long id, String nombres, String apellidos,
    String tipoDocumento, String numeroDocumento, String telefono, String direccion) {}
