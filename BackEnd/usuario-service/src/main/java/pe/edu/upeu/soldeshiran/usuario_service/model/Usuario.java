package pe.edu.upeu.soldeshiran.usuario_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity @Data
@Table(name = "usuario")
public class Usuario {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario") private Long id;
  @Column(name = "nombre_usuario", unique = true, nullable = false, length = 80)
  private String nombre;
  @Column(unique = true, nullable = false, length = 150) private String email;
  @Column(nullable = false, length = 255) private String password;
  @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
  private Boolean activo = true;
  @Column(name = "foto_perfil", length = 500) private String fotoPerfil;
  @Column(name = "fecha_creacion", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime fechaCreacion;
  @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "id_persona", unique = true, foreignKey = @ForeignKey(name = "fk_usuario_persona"))
  @OnDelete(action = OnDeleteAction.SET_NULL)
  private Persona persona;
  @ManyToOne(optional = false)
  @JoinColumn(name = "id_rol", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_rol"))
  private Rol rol;

  @PrePersist void crear() { if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(); }
}
