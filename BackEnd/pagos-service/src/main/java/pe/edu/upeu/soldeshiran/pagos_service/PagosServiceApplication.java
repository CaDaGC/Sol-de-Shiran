package pe.edu.upeu.soldeshiran.pagos_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.context.annotation.Import(pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoConfig.class)
@SpringBootApplication
@EnableFeignClients
public class PagosServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(PagosServiceApplication.class, args);
	}
}