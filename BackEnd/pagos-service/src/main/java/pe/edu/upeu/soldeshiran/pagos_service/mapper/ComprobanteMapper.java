package pe.edu.upeu.soldeshiran.pagos_service.mapper;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
@Component
public class ComprobanteMapper {
  public ComprobanteDatos snapshot(String numero,java.time.LocalDateTime ahora,TransaccionPago tx,DatosReservaComprobante r,JsonNode usuario){
    var persona=usuario.path("persona");
    return new ComprobanteDatos(numero,ahora,r.idUsuario(),persona.isObject()?valor(persona,"nombres")+" "+valor(persona,"apellidos"):"Datos personales no registrados",
      valor(persona,"tipoDocumento"),valor(persona,"numeroDocumento"),valor(persona,"telefono"),valor(persona,"direccion"),valor(usuario,"email"),
      r.codigo(),r.nombreCabana(),r.fechaIngreso(),r.fechaSalida(),tx.getIdTransaccion(),tx.getIdExternoPasarela(),tx.getFechaTransaccion(),tx.getMoneda(),tx.getMonto(),r.montoTotal(),r.montoTotal().subtract(tx.getMonto()));
  }
  private String valor(JsonNode n,String campo){var valor=n.path(campo).asText("").strip();return valor.isEmpty()?"No registrado":valor;}
}
