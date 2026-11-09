package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaResumenResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CabanaResumenController {
    private final CabanaRepository cabanaRepository;

    @GetMapping("/internal/cabanas/resumen")
    public List<CabanaResumenResponse> consultar(@RequestParam List<Long> ids) {
        if (ids.isEmpty() || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica de 1 a 100 IDs válidos.");
        }
        return cabanaRepository.resumirPorIds(ids);
    }
}
