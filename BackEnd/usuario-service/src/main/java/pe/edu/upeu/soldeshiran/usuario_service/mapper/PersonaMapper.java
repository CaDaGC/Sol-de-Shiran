package pe.edu.upeu.soldeshiran.usuario_service.mapper;
import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.usuario_service.dto.*;
import pe.edu.upeu.soldeshiran.usuario_service.model.Persona;
@Component
public class PersonaMapper {
  public void actualizar(Persona persona, PersonaRequest r) {
    persona.setNombres(r.getNombres().strip());
    persona.setApellidos(r.getApellidos().strip());
    persona.setTipoDocumento(r.getTipoDocumento().strip().toUpperCase(java.util.Locale.ROOT));
    persona.setNumeroDocumento(r.getNumeroDocumento().strip());
    persona.setTelefono(r.getTelefono().strip());
    persona.setDireccion(r.getDireccion().strip());
  }
  public PersonaResponse toResponse(Persona p) {
    if (p == null) return null;
    var r = new PersonaResponse();
    r.setId(p.getId()); r.setNombres(p.getNombres()); r.setApellidos(p.getApellidos());
    r.setTipoDocumento(p.getTipoDocumento()); r.setNumeroDocumento(p.getNumeroDocumento());
    r.setTelefono(p.getTelefono()); r.setDireccion(p.getDireccion());
    r.setFechaCreacion(p.getFechaCreacion()); r.setFechaActualizacion(p.getFechaActualizacion());
    return r;
  }
}
