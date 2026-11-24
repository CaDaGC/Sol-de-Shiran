package pe.edu.upeu.soldeshiran.usuario_service.service;
import pe.edu.upeu.soldeshiran.usuario_service.dto.PersonaRequest;
import pe.edu.upeu.soldeshiran.usuario_service.model.Persona;
public interface PersonaService {
  Persona preparar(Persona actual, PersonaRequest solicitud);
}
