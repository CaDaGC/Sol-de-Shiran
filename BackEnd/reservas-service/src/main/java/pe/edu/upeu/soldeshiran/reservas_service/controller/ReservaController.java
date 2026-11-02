package pe.edu.upeu.soldeshiran.reservas_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CotizacionReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.FechasOcupadasResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Validated
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping("/cabanas/{idCabana}/disponibilidad")
    public ResponseEntity<CotizacionReservaResponse> consultarDisponibilidad(
            @PathVariable @Positive Long idCabana,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaIngreso,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaSalida,
            @RequestParam @Min(1) Integer cantidadHuespedes
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reservaService.consultarDisponibilidad(
                        idCabana,
                        fechaIngreso,
                        fechaSalida,
                        cantidadHuespedes
                ));
    }

    @GetMapping("/cabanas/{idCabana}/fechas-ocupadas")
    public ResponseEntity<FechasOcupadasResponse> consultarFechasOcupadas(
            @PathVariable @Positive Long idCabana,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reservaService.consultarFechasOcupadas(
                        idCabana,
                        desde,
                        hasta
                ));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crearReservaTemporal(
            @RequestHeader("X-User-Id") @Positive Long idUsuario,
            @Valid @RequestBody CrearReservaRequest request
    ) {
        ReservaResponse reserva =
                reservaService.crearReservaTemporal(idUsuario, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(reserva);
    }
}