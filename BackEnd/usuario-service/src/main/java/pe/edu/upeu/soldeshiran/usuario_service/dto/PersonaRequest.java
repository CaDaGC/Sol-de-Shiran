package pe.edu.upeu.soldeshiran.usuario_service.dto;
import lombok.Data;
@Data
public class PersonaRequest {
  private Long id;
  private String nombres;
  private String apellidos;
  @com.fasterxml.jackson.annotation.JsonAlias("tipo_documento")
  private String tipoDocumento = "DNI";
  @com.fasterxml.jackson.annotation.JsonAlias("numero_documento")
  private String numeroDocumento;
  private String telefono;
  private String direccion;
}
