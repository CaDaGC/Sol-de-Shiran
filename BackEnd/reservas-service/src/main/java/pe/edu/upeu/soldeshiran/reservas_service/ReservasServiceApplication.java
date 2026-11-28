package pe.edu.upeu.soldeshiran.reservas_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@org.springframework.context.annotation.Import(pe.edu.upeu.soldeshiran.coordinacion.CandadoReserva.class)
@EnableFeignClients
@EnableScheduling
public class ReservasServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservasServiceApplication.class, args);
	}
}