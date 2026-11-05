package pe.edu.upeu.soldeshiran.catalogo_service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
@Service @RequiredArgsConstructor
public class OcupacionService {
 private final CabanaRepository repository;
 @Transactional public String ocupar(Long id, Long reserva) {
  var c=repository.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No existe la cabaña."));
  if(reserva.equals(c.getIdReservaOcupante())) return c.getEstadoOcupacion();
  if(c.getEstado()!=EstadoCabana.ACTIVA || c.getIdReservaOcupante()!=null)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La cabaña no está disponible para check-in.");
  c.setIdReservaOcupante(reserva); repository.saveAndFlush(c); return c.getEstadoOcupacion();
 }
}
