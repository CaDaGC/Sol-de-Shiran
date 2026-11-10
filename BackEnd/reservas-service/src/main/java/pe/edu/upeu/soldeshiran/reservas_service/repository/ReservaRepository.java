package pe.edu.upeu.soldeshiran.reservas_service.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {
  @Query("select distinct r.idCabana from Reserva r")
  List<Long> listarCabanasConReservas();

  @Query("""
      select r.idReserva from Reserva r
      where r.idUsuario = :usuario and r.idCabana = :cabana
        and r.estadoReserva = pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva.FINALIZADA
        and r.checkout.fecha is not null
      order by r.checkout.fecha, r.idReserva
      """)
  List<Long> listarEstanciasFinalizadas(@Param("usuario") Long usuario, @Param("cabana") Long cabana);

  interface CobroDanioPendiente {
    Long getIdReserva();
    Long getRegistradoPor();
    String getDescripcion();
    java.math.BigDecimal getMonto();
    String getMedioPago();
    LocalDateTime getFecha();
  }

  @Query("""
      select r.idReserva as idReserva, r.checkout.registradoPor as registradoPor,
             r.checkout.descripcionDanos as descripcion, r.checkout.montoDanos as monto,
             r.checkout.medioPago as medioPago, r.checkout.fecha as fecha
      from Reserva r
      where r.estadoReserva = pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva.FINALIZADA
        and r.checkout.montoDanos > 0
        and (r.checkoutPagoDanosExportado is null or r.checkoutPagoDanosExportado = false)
      order by r.idReserva
      """)
  List<CobroDanioPendiente> listarCobrosDaniosPendientes();

  @org.springframework.transaction.annotation.Transactional
  @Modifying
  @Query("update Reserva r set r.checkoutPagoDanosExportado = true where r.idReserva = :id")
  void marcarCobroDanioExportado(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.idReserva = :id")
    java.util.Optional<Reserva> bloquearPorId(@Param("id") Long id);


  @Query(
      """
      SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
      FROM Reserva r
      WHERE r.idCabana = :idCabana
        AND r.fechaIngreso < :fechaSalida
        AND r.fechaSalida > :fechaIngreso
        AND (
              r.estadoReserva IN :estadosBloqueantes
              OR (
                  r.estadoReserva = :pendientePago
                  AND r.expiraEn > :ahora
              )
        )
      """)
  boolean existeCruceDeFechas(
      @Param("idCabana") Long idCabana,
      @Param("fechaIngreso") LocalDate fechaIngreso,
      @Param("fechaSalida") LocalDate fechaSalida,
      @Param("estadosBloqueantes") List<EstadoReserva> estadosBloqueantes,
      @Param("pendientePago") EstadoReserva pendientePago,
      @Param("ahora") LocalDateTime ahora);

  @Query(
      """
      SELECT r
      FROM Reserva r
      WHERE r.idCabana = :idCabana
        AND r.fechaIngreso <= :hasta
        AND r.fechaSalida > :desde
        AND (
              r.estadoReserva IN :estadosBloqueantes
              OR (
                  r.estadoReserva = :pendientePago
                  AND r.expiraEn > :ahora
              )
        )
      ORDER BY r.fechaIngreso ASC
      """)
  List<Reserva> buscarReservasOcupadasEnRango(
      @Param("idCabana") Long idCabana,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("estadosBloqueantes") List<EstadoReserva> estadosBloqueantes,
      @Param("pendientePago") EstadoReserva pendientePago,
      @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("""
            DELETE FROM Reserva r
            WHERE r.estadoReserva = :expirada
               OR (r.estadoReserva = :pendientePago
                   AND r.expiraEn <= :ahora
                   AND (r.totalPagado IS NULL OR r.totalPagado = 0))
            """)
    int eliminarReservasVencidas(
            @Param("pendientePago") EstadoReserva pendientePago,
            @Param("expirada") EstadoReserva expirada,
            @Param("ahora") LocalDateTime ahora
    );
}
