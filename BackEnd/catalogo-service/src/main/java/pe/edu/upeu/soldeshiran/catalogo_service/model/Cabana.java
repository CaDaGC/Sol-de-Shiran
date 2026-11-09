package pe.edu.upeu.soldeshiran.catalogo_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cabana")
@Getter
@Setter
@NoArgsConstructor
public class Cabana {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_cabana")
  private Long idCabana;

  private Long duenoId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "cabana_trabajadores", joinColumns = @JoinColumn(name = "id_cabana"))
  @Column(name = "trabajador_id")
  private java.util.Set<Long> trabajadores = new java.util.HashSet<>();

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String descripcion;

  @Column(name = "aforo_maximo", nullable = false)
  private Integer aforoMaximo;

  @Column(name = "num_habitaciones", nullable = false)
  private Integer numHabitaciones;

  @Column(name = "num_banos", nullable = false)
  private Integer numBanos;

  @Column(name = "tarifa_noche", nullable = false, precision = 10, scale = 2)
  private BigDecimal tarifaNoche;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCabana estado = EstadoCabana.ACTIVA;

    @Column(name="id_reserva_ocupante")
    private Long idReservaOcupante;

    public String getEstadoOcupacion() { return idReservaOcupante == null ? "DISPONIBLE" : "OCUPADA"; }

    @Column(name = "imagen_principal", nullable = false, length = 255)
    private String imagenPrincipal;

  @Column(name = "fecha_creacion", insertable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @OneToMany(
      mappedBy = "cabana",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<CabanaImagen> imagenes = new ArrayList<>();
}
