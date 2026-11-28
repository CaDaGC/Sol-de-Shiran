package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import feign.FeignException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.client.UsuariosClient;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.AsignacionRestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.GestionRestauranteRequest.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.CartaMenuMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.RestauranteMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.CategoriaMenu;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.ProductoMenu;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Restaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.RestauranteTrabajador;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.CategoriaMenuRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.ProductoMenuRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteTrabajadorRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.AccesoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.GestionRestauranteService;
import pe.edu.upeu.soldeshiran.restaurante_service.service.HorarioRestauranteService;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionRestauranteServiceImpl
        implements GestionRestauranteService {

    private final RestauranteRepository restaurantes;
    private final CategoriaMenuRepository categorias;
    private final ProductoMenuRepository productos;
    private final RestauranteTrabajadorRepository asignaciones;
    private final RestauranteMapper restauranteMapper;
    private final CartaMenuMapper cartaMapper;
    private final UsuariosClient usuarios;
    private final AccesoRestaurante acceso;
    private final HorarioRestauranteService horarios;

    @Value("${seguridad.internal-key}")
    private String claveInterna;

    @Override
    @Transactional(readOnly = true)
    public List<RestauranteResponse> listarRestaurantes(Actor actor) {
        actor.exigir("ADMIN");

        return restaurantes.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        Restaurante::getNombre,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(restauranteMapper::toResponse)
                .toList();
    }

    @Override
    public RestauranteResponse crearRestaurante(
            RestauranteDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");

        var restaurante = new Restaurante();
        aplicarRestaurante(restaurante, datos);
        restaurante.setEstado(EstadoRestaurante.ACTIVO);

        return restauranteMapper.toResponse(
                restaurantes.saveAndFlush(restaurante)
        );
    }

    @Override
    public RestauranteResponse editarRestaurante(
            Long idRestaurante,
            RestauranteDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");

        var restaurante = bloquearRestaurante(idRestaurante);
        aplicarRestaurante(restaurante, datos);

        return restauranteMapper.toResponse(
                restaurantes.saveAndFlush(restaurante)
        );
    }

    @Override
    public RestauranteResponse cambiarEstado(
            Long idRestaurante,
            EstadoDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");

        var restaurante = bloquearRestaurante(idRestaurante);
        restaurante.setEstado(datos.estado());

        return restauranteMapper.toResponse(
                restaurantes.saveAndFlush(restaurante)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaMenuResponse> listarCategorias(
            Long idRestaurante,
            Actor actor) {

        actor.exigir("ADMIN");
        buscarRestaurante(idRestaurante);

        return categorias
                .findByRestauranteIdRestauranteOrderByTipoAscNombreAscIdCategoriaAsc(
                        idRestaurante
                )
                .stream()
                .map(cartaMapper::toResponse)
                .toList();
    }

    @Override
    public CategoriaMenuResponse guardarCategoria(
            Long idRestaurante,
            Long idCategoria,
            CategoriaDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");

        var restaurante = bloquearRestaurante(idRestaurante);

        var categoria = idCategoria == null
                ? new CategoriaMenu()
                : buscarCategoria(idRestaurante, idCategoria);

        categoria.setRestaurante(restaurante);
        categoria.setNombre(datos.nombre().strip());
        categoria.setTipo(datos.tipo());

        return cartaMapper.toResponse(
                categorias.saveAndFlush(categoria)
        );
    }

    @Override
    public void eliminarCategoria(
            Long idRestaurante,
            Long idCategoria,
            Actor actor) {

        actor.exigir("ADMIN");
        bloquearRestaurante(idRestaurante);

        var categoria = buscarCategoria(idRestaurante, idCategoria);

        if (productos.existsByCategoriaIdCategoria(idCategoria)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puedes eliminar una categoría que tiene productos"
            );
        }

        categorias.delete(categoria);
        categorias.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMenuResponse> listarProductos(
            Long idRestaurante,
            Actor actor) {

        actor.exigir("ADMIN");
        buscarRestaurante(idRestaurante);

        return productos.consultarCarta(idRestaurante, null, null)
                .stream()
                .map(cartaMapper::toResponse)
                .toList();
    }

    @Override
    public ProductoMenuResponse guardarProducto(
            Long idRestaurante,
            Long idProducto,
            ProductoDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");

        var restaurante = bloquearRestaurante(idRestaurante);
        var categoria = buscarCategoria(
                idRestaurante,
                datos.idCategoria()
        );

        var producto = idProducto == null
                ? new ProductoMenu()
                : buscarProducto(idRestaurante, idProducto);

        producto.setRestaurante(restaurante);
        producto.setCategoria(categoria);
        producto.setNombre(datos.nombre().strip());
        producto.setDescripcion(normalizar(datos.descripcion()));
        producto.setPrecio(datos.precio());
        producto.setTiempoPreparacionMin(datos.tiempoPreparacionMin());
        producto.setDisponible(datos.disponible());
        producto.setImagenUrl(normalizar(datos.imagenUrl()));

        return cartaMapper.toResponse(
                productos.saveAndFlush(producto)
        );
    }

    @Override
    public ProductoMenuResponse cambiarDisponibilidad(
            Long idRestaurante,
            Long idProducto,
            DisponibilidadDatos datos,
            Actor actor) {

        acceso.exigir(idRestaurante, actor);
        bloquearRestaurante(idRestaurante);

        var producto = buscarProducto(idRestaurante, idProducto);
        producto.setDisponible(datos.disponible());

        return cartaMapper.toResponse(
                productos.saveAndFlush(producto)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionRestauranteResponse> listarAsignaciones(
            Long idRestaurante,
            Actor actor) {

        actor.exigir("ADMIN");
        buscarRestaurante(idRestaurante);

        return asignaciones
                .findByRestauranteIdRestauranteOrderByIdAsignacionAsc(
                        idRestaurante
                )
                .stream()
                .map(this::mapearAsignacion)
                .toList();
    }

    @Override
    public AsignacionRestauranteResponse asignar(
            Long idRestaurante,
            AsignacionDatos datos,
            Actor actor) {

        actor.exigir("ADMIN");
        validarEncargado(datos.idUsuario());

        var restaurante = bloquearRestaurante(idRestaurante);

        var asignacion = asignaciones.findByIdUsuario(
                datos.idUsuario()
        ).orElseGet(RestauranteTrabajador::new);

        asignacion.setIdUsuario(datos.idUsuario());
        asignacion.setRestaurante(restaurante);

        return mapearAsignacion(
                asignaciones.saveAndFlush(asignacion)
        );
    }

    @Override
    public void retirarAsignacion(
            Long idRestaurante,
            Long idUsuario,
            Actor actor) {

        actor.exigir("ADMIN");
        bloquearRestaurante(idRestaurante);
        exigirPositivo(idUsuario);

        var asignacion = asignaciones.findByIdUsuario(idUsuario)
                .filter(actual -> actual.getRestaurante()
                        .getIdRestaurante()
                        .equals(idRestaurante))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no está asignado a este restaurante"
                ));

        asignaciones.delete(asignacion);
        asignaciones.flush();
    }

    private void aplicarRestaurante(
            Restaurante restaurante,
            RestauranteDatos datos) {

        restaurante.setNombre(datos.nombre().strip());
        restaurante.setEspecialidad(normalizar(datos.especialidad()));
        restaurante.setImagenUrl(normalizar(datos.imagenUrl()));
        restaurante.setHorarioAtencion(horarios.describirHorario());
    }

    private Restaurante buscarRestaurante(Long id) {
        exigirPositivo(id);

        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurante no encontrado"
                ));
    }

    private Restaurante bloquearRestaurante(Long id) {
        exigirPositivo(id);

        return restaurantes.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurante no encontrado"
                ));
    }

    private CategoriaMenu buscarCategoria(
            Long idRestaurante,
            Long idCategoria) {

        exigirPositivo(idCategoria);

        return categorias
                .findByIdCategoriaAndRestauranteIdRestaurante(
                        idCategoria,
                        idRestaurante
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La categoría no pertenece al restaurante"
                ));
    }

    private ProductoMenu buscarProducto(
            Long idRestaurante,
            Long idProducto) {

        exigirPositivo(idProducto);

        return productos.findById(idProducto)
                .filter(producto -> producto.getRestaurante()
                        .getIdRestaurante()
                        .equals(idRestaurante))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El producto no pertenece al restaurante"
                ));
    }

    private void validarEncargado(Long idUsuario) {
        exigirPositivo(idUsuario);

        try {
            var identidad = usuarios.identidad(
                    idUsuario,
                    claveInterna
            );

            if (identidad == null
                    || !identidad.activo()
                    || !"RESTAURANTE".equals(
                    Actor.normalizar(identidad.rol())
            )) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selecciona una cuenta activa con rol RESTAURANTE"
                );
            }
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
            );
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar la cuenta del encargado",
                    exception
            );
        }
    }

    private AsignacionRestauranteResponse mapearAsignacion(
            RestauranteTrabajador asignacion) {

        return new AsignacionRestauranteResponse(
                asignacion.getIdAsignacion(),
                asignacion.getRestaurante().getIdRestaurante(),
                asignacion.getIdUsuario()
        );
    }

    private void exigirPositivo(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser positivo"
            );
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.strip();
    }
}