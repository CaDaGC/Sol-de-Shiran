package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.almacenamiento.*;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.*;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.FotoMenuService;

@Service
@RequiredArgsConstructor
@Transactional
public class FotoMenuServiceImpl implements FotoMenuService {
    private final RestauranteRepository restaurantes;
    private final ProductoMenuRepository productos;
    private final AlmacenamientoService archivos;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entidades;

    @Override
    public String guardar(String tipo, Long id, MultipartFile archivo, Actor actor) {
        actor.exigir("ADMIN");
        var categoria = categoria(tipo, id);
        byte[] contenido = convertir(archivo);
        String referencia;
        String anterior;
        String url = "/api/restaurante/imagenes/" + tipo + "/" + id;
        if (categoria == CategoriaArchivo.RESTAURANTE) {
            var local = restaurantes.bloquearPorId(id).orElseThrow(this::noEncontrado);
            anterior = local.getImagenArchivo();
            referencia = archivos.guardar(categoria, UUID.randomUUID() + ".png", contenido, "image/png");
            local.setImagenArchivo(referencia);
            local.setImagenUrl(url);
            restaurantes.saveAndFlush(local);
        } else {
            var producto = productos.findById(id).orElseThrow(this::noEncontrado);
            restaurantes.bloquearPorId(producto.getRestaurante().getIdRestaurante()).orElseThrow(this::noEncontrado);
            entidades.refresh(producto);
            anterior = producto.getImagenArchivo();
            referencia = archivos.guardar(categoria, UUID.randomUUID() + ".png", contenido, "image/png");
            producto.setImagenArchivo(referencia);
            producto.setImagenUrl(url);
            productos.saveAndFlush(producto);
        }
        retirarAlConfirmar(categoria, anterior);
        return url;
    }

    @Override
    public void quitar(String tipo, Long id, Actor actor) {
        actor.exigir("ADMIN");
        var categoria = categoria(tipo, id);
        String anterior;
        if (categoria == CategoriaArchivo.RESTAURANTE) {
            var local = restaurantes.bloquearPorId(id).orElseThrow(this::noEncontrado);
            anterior = local.getImagenArchivo();
            local.setImagenArchivo(null);
            local.setImagenUrl(null);
            restaurantes.saveAndFlush(local);
        } else {
            var producto = productos.findById(id).orElseThrow(this::noEncontrado);
            restaurantes.bloquearPorId(producto.getRestaurante().getIdRestaurante()).orElseThrow(this::noEncontrado);
            entidades.refresh(producto);
            anterior = producto.getImagenArchivo();
            producto.setImagenArchivo(null);
            producto.setImagenUrl(null);
            productos.saveAndFlush(producto);
        }
        retirarAlConfirmar(categoria, anterior);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] leer(String tipo, Long id) {
        var categoria = categoria(tipo, id);
        String referencia = categoria == CategoriaArchivo.RESTAURANTE
                ? restaurantes.findById(id).orElseThrow(this::noEncontrado).getImagenArchivo()
                : productos.findById(id).orElseThrow(this::noEncontrado).getImagenArchivo();
        if (referencia == null) throw noEncontrado();
        return archivos.leer(categoria, referencia);
    }

    private CategoriaArchivo categoria(String tipo, Long id) {
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        return switch (tipo) {
            case "locales" -> CategoriaArchivo.RESTAURANTE;
            case "productos" -> CategoriaArchivo.PRODUCTO;
            default -> throw noEncontrado();
        };
    }

    private byte[] convertir(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > 2 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen JPG o PNG de hasta 2 MB");
        }
        try (var entrada = ImageIO.createImageInputStream(archivo.getInputStream())) {
            if (entrada == null) throw new IOException();
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new IOException();
            var lector = lectores.next();
            try {
                lector.setInput(entrada);
                String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("png") && !formato.equals("jpeg")) throw new IOException();
                if ((long) lector.getWidth(0) * lector.getHeight(0) > 16000000) throw new IOException();
                var imagen = lector.read(0);
                if (imagen == null) throw new IOException();
                var salida = new ByteArrayOutputStream();
                if (!ImageIO.write(imagen, "png", salida)) throw new IOException();
                return salida.toByteArray();
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe ser JPG o PNG válido de hasta 16 megapíxeles");
        }
    }

    private void retirarAlConfirmar(CategoriaArchivo categoria, String referencia) {
        if (referencia == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                archivos.descartar(categoria, referencia);
            }
        });
    }

    private ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen o elemento no encontrado");
    }
}
