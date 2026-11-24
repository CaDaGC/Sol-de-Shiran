package pe.edu.upeu.soldeshiran.usuario_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "rol")
@Data
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String nombre;


}
