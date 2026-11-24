package pe.edu.upeu.soldeshiran.resenas_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ResenasServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResenasServiceApplication.class, args);
    }
}
