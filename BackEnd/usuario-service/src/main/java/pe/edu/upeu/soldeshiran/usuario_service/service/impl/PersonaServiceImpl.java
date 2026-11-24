package pe.edu.upeu.soldeshiran.usuario_service.service.impl;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.dto.PersonaRequest;
import pe.edu.upeu.soldeshiran.usuario_service.model.Persona;
import pe.edu.upeu.soldeshiran.usuario_service.repository.PersonaRepository;
import pe.edu.upeu.soldeshiran.usuario_service.mapper.PersonaMapper;
import pe.edu.upeu.soldeshiran.usuario_service.service.PersonaService;
@Service @RequiredArgsConstructor
public class PersonaServiceImpl implements PersonaService {
  private final PersonaRepository personas;
  private final PersonaMapper mapper;

  @Override public Persona preparar(Persona actual, PersonaRequest r) {
    if (r == null || invalido(r.getNombres(),100) || invalido(r.getApellidos(),100)
        || invalido(r.getTipoDocumento(),20) || invalido(r.getNumeroDocumento(),20)
        || invalido(r.getTelefono(),20) || invalido(r.getDireccion(),255)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa nombres, apellidos, tipo y número de documento, teléfono y dirección.");
    }
    if (r.getId() != null && (actual == null || !r.getId().equals(actual.getId())))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permite vincular la persona de otra cuenta.");
    personas.findByNumeroDocumento(r.getNumeroDocumento().strip()).ifPresent(otra -> {
      if (actual == null || !Objects.equals(otra.getId(), actual.getId()))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está registrado.");
    });
    var persona = actual == null ? new Persona() : actual;
    mapper.actualizar(persona, r);
    return persona;
  }
  private boolean invalido(String s, int max) { return s == null || s.isBlank() || s.strip().length() > max; }
}
