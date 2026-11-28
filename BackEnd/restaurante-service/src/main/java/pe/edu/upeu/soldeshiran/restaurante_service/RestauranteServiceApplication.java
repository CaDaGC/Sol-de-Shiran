package pe.edu.upeu.soldeshiran.restaurante_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoConfig;

@SpringBootApplication
@EnableFeignClients
@Import(AlmacenamientoConfig.class)
public class RestauranteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestauranteServiceApplication.class, args);
	}
}