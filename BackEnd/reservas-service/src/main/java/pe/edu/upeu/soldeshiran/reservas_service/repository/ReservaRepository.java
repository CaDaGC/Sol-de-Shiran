package pe.edu.upeu.soldeshiran.reservas_service.repository;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
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
            @Param("ahora") LocalDateTime ahora
    );

    @Modifying
    @Query("""
            UPDATE Reserva r
            SET r.estadoReserva = :expirada
            WHERE r.estadoReserva = :pendientePago
              AND r.expiraEn <= :ahora
            """)
    int expirarPendientesVencidas(
            @Param("pendientePago") EstadoReserva pendientePago,
            @Param("expirada") EstadoReserva expirada,
            @Param("ahora") LocalDateTime ahora
    );
}