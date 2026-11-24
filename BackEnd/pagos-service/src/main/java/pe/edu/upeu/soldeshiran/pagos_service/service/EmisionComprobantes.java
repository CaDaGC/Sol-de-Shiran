package pe.edu.upeu.soldeshiran.pagos_service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;
@Component @RequiredArgsConstructor
public class EmisionComprobantes {
  private final ComprobanteService service;
  private final TransaccionPagoRepository pagos;
  private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(EmisionComprobantes.class);
  @EventListener public void aprobado(AdelantoAprobado e){emitir(e.idTransaccion());}
  @Scheduled(fixedDelayString="${comprobantes.reintento-ms:60000}",initialDelayString="${comprobantes.reintento-ms:60000}")
  public void reintentar(){for(Long id:pagos.pendientesComprobante(org.springframework.data.domain.PageRequest.of(0,50)))emitir(id);}
  private void emitir(Long id){try{service.emitir(id);}catch(Exception e){log.warn("Comprobante pendiente para transacción {}: {}",id,e.getClass().getSimpleName());}}
}
