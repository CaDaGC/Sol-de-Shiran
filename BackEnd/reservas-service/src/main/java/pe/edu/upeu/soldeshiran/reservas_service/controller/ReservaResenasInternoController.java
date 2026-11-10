package pe.edu.upeu.soldeshiran.reservas_service.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/reservas/resenas")
public class ReservaResenasInternoController {
    private final ReservaRepository reservas;

    @GetMapping("/estancias")
    public List<Long> listarEstancias(@RequestParam("idUsuario") Long idUsuario,
            @RequestParam("idCabana") Long idCabana) {
        return reservas.listarEstanciasFinalizadas(idUsuario, idCabana);
    }
}
