package pe.edu.upeu.soldeshiran.pagos_service.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import pe.edu.upeu.soldeshiran.pagos_service.model.*;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.repository.*;
import pe.edu.upeu.soldeshiran.pagos_service.service.*;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.security.*;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService;
import pe.edu.upeu.soldeshiran.almacenamiento.CategoriaArchivo;
@Service @RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {
  private final ComprobantePagoRepository comprobantes;
  private final TransaccionPagoRepository pagos;
  private final ReservaInternaClient reservas;
  private final ObjectMapper mapper;
  private final ComprobantePdf generador;
  private final pe.edu.upeu.soldeshiran.pagos_service.mapper.ComprobanteMapper datosMapper;
  @Value("${servicios.interno.token}") private String token;
  @Value("${seguridad.internal-key}") private String key;
  @Value("${usuarios.url:http://localhost:8087}") private String usuariosUrl;
  private final AlmacenamientoService almacenamiento;

  @Override @Transactional
  public ComprobantePago consultar(Long idReserva,Actor actor){
    actor.exigir("CLIENTE","ADMIN","TRABAJADOR");
    if(actor.es("ADMIN","TRABAJADOR")) {
      var reserva=reservas.datosComprobante(idReserva,token);
      if(!java.util.Set.of("OCUPADA","FINALIZADA").contains(reserva.estadoReserva()))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El comprobante del adelanto estará disponible para el personal después del check-in.");
    }
    var existente=comprobantes.findByIdReserva(idReserva);
    if(existente.isPresent()){exigirPropietario(existente.get().getIdUsuario(),actor);return existente.get();}
    var r=reservas.datosComprobante(idReserva,token);exigirPropietario(r.idUsuario(),actor);
    var tx=pagos.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(idReserva,TransaccionPago.ONLINE_ADELANTO,EstadoTransaccion.APROBADA)
      .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No hay un adelanto aprobado para esta reserva."));
    return generar(tx.getIdTransaccion());
  }
  private void exigirPropietario(Long id,Actor actor){if(actor.es("CLIENTE")&&!id.equals(actor.id()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Comprobante no encontrado.");}
  @Override @Transactional public void emitir(Long idTransaccion){generar(idTransaccion);}
  private ComprobantePago generar(Long idTransaccion){
    var tx=pagos.bloquearComprobante(idTransaccion).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!TransaccionPago.ONLINE_ADELANTO.equals(tx.getTipoPago())||tx.getEstadoTransaccion()!=EstadoTransaccion.APROBADA)
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se emiten comprobantes de adelantos aprobados.");
    var existente=comprobantes.findByIdReserva(tx.getIdReserva());if(existente.isPresent())return existente.get();
    var r=reservas.datosComprobante(tx.getIdReserva(),token);
    var usuario=InternalHttp.cliente(usuariosUrl).get().uri("/internal/usuarios/"+r.idUsuario()+"/perfil")
      .header("X-Internal-Key",key).retrieve().body(JsonNode.class);
    if(usuario==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudieron obtener los datos del cliente.");
    var ahora=java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    String numero=String.format(java.util.Locale.ROOT,"CP-%08d",tx.getIdTransaccion());
    var d=datosMapper.snapshot(numero,ahora,tx,r,usuario);
    String archivo=null;
    try{
      var c=new ComprobantePago();c.setIdTransaccion(tx.getIdTransaccion());c.setIdReserva(tx.getIdReserva());c.setIdUsuario(r.idUsuario());c.setNumero(numero);c.setFechaEmision(ahora);
      c.setDatosJson(mapper.writeValueAsString(d));
      archivo=almacenamiento.guardar(CategoriaArchivo.COMPROBANTE,java.util.UUID.randomUUID()+".pdf",generador.generar(d),"application/pdf");
      c.setArchivo(archivo);
      return comprobantes.saveAndFlush(c);
    }catch(java.io.IOException|RuntimeException e){
      if(archivo!=null)almacenamiento.descartar(CategoriaArchivo.COMPROBANTE,archivo);
      if(e instanceof ResponseStatusException re)throw re;
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo emitir el comprobante. Puedes reintentar sin volver a pagar.",e);
    }
  }
  @Override public byte[] pdf(ComprobantePago comprobante) {
    return almacenamiento.leer(CategoriaArchivo.COMPROBANTE, comprobante.getArchivo());
  }
  @Transactional public boolean migrar(Long id) {
    var comprobante = comprobantes.findById(id).orElseThrow();
    if (comprobante.getArchivo().startsWith("drive:")) return false;
    byte[] contenido = pdf(comprobante);
    String referencia = almacenamiento.migrar(CategoriaArchivo.COMPROBANTE, comprobante.getNumero() + ".pdf", contenido, "application/pdf");
    comprobante.setArchivo(referencia);
    comprobantes.saveAndFlush(comprobante);
    return true;
  }
}
