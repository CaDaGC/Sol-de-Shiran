package pe.edu.upeu.soldeshiran.usuario_service.dto;
import lombok.Data;
@Data
public class PersonaResponse {
  private Long id;
  private String nombres;
  private String apellidos;
  private String tipoDocumento = "DNI";
  private String numeroDocumento;
  private String telefono;
  private String direccion;
}
