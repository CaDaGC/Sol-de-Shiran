package pe.edu.upeu.soldeshiran.auth_service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.soldeshiran.auth_service.security.JwtService;
import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
class JwtDuracionTest {
  @Test void limitaLaDuracionAUnaHoraInclusoConConfiguracionAntigua() {
    var servicio=new JwtService();String secreto="clave-de-pruebas-suficientemente-larga-para-firmar-un-jwt";
    ReflectionTestUtils.setField(servicio,"secret",secreto);
    ReflectionTestUtils.setField(servicio,"expiration",86_400_000L);
    var usuario=new UsuarioResponse();usuario.setId(1L);usuario.setEmail("test@example.com");usuario.setRol("CLIENTE");
    var claims=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
      .build().parseSignedClaims(servicio.generateToken(usuario)).getPayload();
    long duracion=claims.getExpiration().getTime()-claims.getIssuedAt().getTime();
    assertTrue(duracion>0 && duracion<=3_600_000L);
  }
}
