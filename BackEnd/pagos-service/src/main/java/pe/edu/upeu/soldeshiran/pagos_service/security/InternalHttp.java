package pe.edu.upeu.soldeshiran.pagos_service.security;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class InternalHttp {
  private InternalHttp() {}

  public static RestClient cliente(String url) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
