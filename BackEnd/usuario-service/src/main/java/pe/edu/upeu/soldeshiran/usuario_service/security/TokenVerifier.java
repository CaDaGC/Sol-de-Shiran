package pe.edu.upeu.soldeshiran.usuario_service.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenVerifier {
  private final ObjectMapper json;
  private final String secret;

  public TokenVerifier(
      ObjectMapper json,
      @Value("${jwt.secret:ESTA_ES_UNA_CLAVE_SECRETA_MUY_LARGA_PARA_FIRMAR_TOKENS_123456}")
          String secret) {
    this.json = json;
    this.secret = secret;
  }

  public Long verificar(String authorization) {
    try {
      if (authorization == null || !authorization.startsWith("Bearer "))
        throw new IllegalArgumentException();
      String[] parts = authorization.substring(7).split("\\.");
      if (parts.length != 3) throw new IllegalArgumentException();
      JsonNode header = json.readTree(Base64.getUrlDecoder().decode(parts[0]));
      String algorithm =
          switch (header.path("alg").asText()) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new IllegalArgumentException();
          };
      Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
      byte[] expected =
          mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
      if (!MessageDigest.isEqual(expected, Base64.getUrlDecoder().decode(parts[2])))
        throw new IllegalArgumentException();
      JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
      long now = java.time.Instant.now().getEpochSecond();
      if (claims.path("exp").asLong(0) <= now || claims.path("nbf").asLong(0) > now)
        throw new IllegalArgumentException();
      long id = claims.path("userId").asLong(0);
      if (id < 1) throw new IllegalArgumentException();
      return id;
    } catch (Exception ex) {
      throw new IllegalArgumentException("Token inválido o vencido");
    }
  }
}
