package pe.edu.upeu.soldeshiran.catalogo_service.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.service.OcupacionService;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/cabanas/interno")
public class OcupacionInternaController {
 private final OcupacionService service;
 @Value("${servicios.interno.token}") private String token;
 @PostMapping("/{idCabana}/ocupacion/{idReserva}")
 public String ocupar(@PathVariable Long idCabana, @PathVariable Long idReserva,
   @RequestHeader("X-Internal-Service-Token") String recibido) {
  validarToken(recibido);
  if(idReserva<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reserva inválida.");
  return service.ocupar(idCabana,idReserva);
 }
    @DeleteMapping("/{idCabana}/ocupacion/{idReserva}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable Long idCabana, @PathVariable Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String recibido) {
        validarToken(recibido);
        service.liberar(idCabana, idReserva);
    }

    private void validarToken(String recibido) {
        if (!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                recibido.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna inválida.");
        }
    }
}
