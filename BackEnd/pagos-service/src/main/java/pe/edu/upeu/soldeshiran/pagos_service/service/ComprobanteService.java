package pe.edu.upeu.soldeshiran.pagos_service.service;
import pe.edu.upeu.soldeshiran.pagos_service.model.ComprobantePago;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;
public interface ComprobanteService {
  ComprobantePago consultar(Long idReserva, Actor actor);
  void emitir(Long idTransaccion);
  byte[] pdf(ComprobantePago comprobante);
}
