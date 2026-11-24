package pe.edu.upeu.soldeshiran.usuario_service.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.dto.DisponibilidadNombreResponse;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NombreUsuarioService {
  private final UsuarioRepository usuarios;

  public DisponibilidadNombreResponse consultar(String nombre) {
    if (nombre == null || nombre.isBlank() || nombre.strip().length() > 80)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Escribe un nombre de usuario de entre 1 y 80 caracteres.");
    String limpio = nombre.strip();
    if (!usuarios.existsByNombreIgnoreCase(limpio))
      return new DisponibilidadNombreResponse(limpio, true, List.of());

    String base = Normalizer.normalize(limpio, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}._-]", "");
    if (base.isBlank()) base = "usuario";
    base = base.substring(0, Math.min(base.length(), 74));
    List<String> sugerencias = new ArrayList<>();
    for (int numero = 1; numero <= 40 && sugerencias.size() < 2; numero++) {
      String candidato = base + "_" + numero;
      if (!usuarios.existsByNombreIgnoreCase(candidato)) sugerencias.add(candidato);
    }
    while (sugerencias.size() < 2) {
      String candidato = base.substring(0, Math.min(base.length(), 70)) + "_"
          + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
      if (!usuarios.existsByNombreIgnoreCase(candidato) && !sugerencias.contains(candidato))
        sugerencias.add(candidato);
    }
    return new DisponibilidadNombreResponse(limpio, false, sugerencias);
  }
}
