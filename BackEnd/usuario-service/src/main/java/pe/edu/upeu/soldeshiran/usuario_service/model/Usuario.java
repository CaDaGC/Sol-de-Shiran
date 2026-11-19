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

  @Column(columnDefinition = "boolean default true")
  private Boolean activo = true;

  private Long duenoId;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Lob @Column(columnDefinition = "LONGBLOB")
  private byte[] fotoPerfil;

  @com.fasterxml.jackson.annotation.JsonIgnore
  private String tipoFotoPerfil;

  @ManyToOne
  @JoinColumn(name = "rol_id")
  private Rol rol;
}
