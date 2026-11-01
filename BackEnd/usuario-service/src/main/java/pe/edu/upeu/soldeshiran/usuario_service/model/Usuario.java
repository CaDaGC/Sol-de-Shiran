package pe.edu.upeu.soldeshiran.usuario_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

}
