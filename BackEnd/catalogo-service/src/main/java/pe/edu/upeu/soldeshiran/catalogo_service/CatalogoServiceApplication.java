package pe.edu.upeu.soldeshiran.catalogo_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@org.springframework.context.annotation.Import(pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoConfig.class)
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogoServiceApplication.class, args);
	}

}
