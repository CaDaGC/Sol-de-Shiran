package pe.edu.upeu.soldeshiran.pagos_service.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import pe.edu.upeu.soldeshiran.pagos_service.service.ComprobanteService;
import pe.edu.upeu.soldeshiran.pagos_service.dto.ComprobanteResponse;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/pagos/reservas/{idReserva}/comprobante")
public class ComprobanteController {
  private final ComprobanteService service;
  @PostMapping public ComprobanteResponse obtener(@PathVariable Long idReserva,jakarta.servlet.http.HttpServletRequest request){
    var c=service.consultar(idReserva,Actor.desde(request));return new ComprobanteResponse(c.getNumero(),c.getNumero()+".pdf",c.getFechaEmision());
  }
  @GetMapping("/pdf") public ResponseEntity<byte[]> pdf(@PathVariable Long idReserva,jakarta.servlet.http.HttpServletRequest request){
    var c=service.consultar(idReserva,Actor.desde(request));return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
      .cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(c.getNumero()+".pdf").build().toString())
      .header("X-Content-Type-Options","nosniff").body(service.pdf(c));
  }
}
