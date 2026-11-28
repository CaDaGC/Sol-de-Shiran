package pe.edu.upeu.soldeshiran.restaurante_service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.coordinacion.CandadoReserva;
import pe.edu.upeu.soldeshiran.restaurante_service.client.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.GestionRestauranteRequest.*;
import pe.edu.upeu.soldeshiran.restaurante_service.model.*;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.*;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.*;

@SpringBootTest(properties = "spring.config.location=classpath:application.properties")
class RestauranteFlujoTest {
    @Autowired GestionRestauranteService gestion;
    @Autowired PedidoService pedidos;
    @Autowired RestauranteConsultaService consultas;
    @Autowired AtencionPedidoService atencion;
    @Autowired HistorialPedidosService historial;
    @Autowired DetallePedidoService detalle;
    @Autowired PanelPedidosService panel;
    @Autowired FotoMenuService fotos;
    @Autowired PedidoRepository repositorio;
    @Autowired ProductoMenuRepository productos;
    @Autowired CategoriaMenuRepository categorias;
    @Autowired RestauranteTrabajadorRepository asignaciones;
    @Autowired RestauranteRepository locales;
    @MockitoBean UsuariosClient usuarios;
    @MockitoBean ReservasClient reservas;
    @MockitoBean CandadoReserva candado;
    @MockitoBean Clock reloj;

    static final Path ARCHIVOS = Path.of(System.getProperty("java.io.tmpdir"), "sol-restaurante-pruebas-" + UUID.randomUUID());
    @DynamicPropertySource
    static void archivos(DynamicPropertyRegistry r) {
        r.add("comandas.directorio", () -> ARCHIVOS.resolve("comandas").toString());
        r.add("restaurantes.fotos.directorio", () -> ARCHIVOS.resolve("locales").toString());
        r.add("productos.fotos.directorio", () -> ARCHIVOS.resolve("productos").toString());
    }

    final Actor admin = new Actor(1L, "ADMIN");
    final Actor cliente = new Actor(42L, "CLIENTE");
    final Actor encargado = new Actor(7L, "RESTAURANTE");
    Long local;
    Long categoria;
    Long producto;

    @BeforeEach
    void preparar() {
        repositorio.deleteAll();
        productos.deleteAll();
        categorias.deleteAll();
        asignaciones.deleteAll();
        locales.deleteAll();
        when(reloj.getZone()).thenReturn(ZoneId.of("America/Lima"));
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-27T17:00:00Z"));
        when(usuarios.identidad(eq(7L), anyString())).thenReturn(new IdentidadUsuarioResponse("RESTAURANTE", true));
        when(reservas.estadias(eq(42L), anyString())).thenReturn(List.of(
                new EstadiaResponse(100L, "RSS00100", 5L, "Cabaña Verano", "Carlos Gonzales")));
        doAnswer(invocacion -> ((Supplier<?>) invocacion.getArgument(1)).get()).when(candado).ejecutar(anyLong(), any());
        local = gestion.crearRestaurante(new RestauranteDatos("Restaurante de prueba", "Regional", null), admin).idRestaurante();
        categoria = gestion.guardarCategoria(local, null, new CategoriaDatos("Platos", TipoCategoria.COMIDA), admin).idCategoria();
        producto = gestion.guardarProducto(local, null, new ProductoDatos(categoria, "Plato", "Descripción",
                new BigDecimal("25.00"), 20, true, null), admin).idProducto();
        gestion.asignar(local, new AsignacionDatos(7L), admin);
    }

    CrearPedidoRequest solicitud(UUID clave) {
        return new CrearPedidoRequest(clave, local, 100L, MetodoPagoPresencial.EFECTIVO,
                List.of(new DetallePedidoRequest(producto, 2, "Sin ají")));
    }

    @Test
    void calculaTotalesYReintentaSinDuplicarYProtegeHistorial() {
        var solicitud = solicitud(UUID.randomUUID());
        var creado = pedidos.crear(solicitud, cliente);
        assertThat(creado.creado()).isTrue();
        assertThat(creado.pedido().montoTotal()).isEqualByComparingTo("50.00");
        assertThat(creado.pedido().codigoPedido()).startsWith("PED");
        assertThat(pedidos.crear(solicitud, cliente).creado()).isFalse();
        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(historial.consultar(42L, 100L, 0, 20, cliente).totalElementos()).isEqualTo(1);
        assertThatThrownBy(() -> historial.consultar(43L, null, 0, 20, cliente))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(() -> detalle.consultar(creado.pedido().idPedido(), new Actor(43L, "CLIENTE")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThat(panel.consultar(local, EstadoPedido.NUEVO, "ver", 0, 20, encargado).totalElementos()).isEqualTo(1);
    }

    @Test
    void atiendePedidoGeneraPdfYConfirmaCobroUnaSolaVez() throws Exception {
        var id = pedidos.crear(solicitud(UUID.randomUUID()), cliente).pedido().idPedido();
        assertThatThrownBy(() -> atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.FINALIZADO, null), encargado))
                .isInstanceOf(ResponseStatusException.class);
        atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.EN_PREPARACION, null), encargado);
        byte[] pdf = atencion.descargarComanda(local, id, encargado);
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.EN_PREPARACION, null), encargado);
        try (var archivos = Files.list(ARCHIVOS.resolve("comandas"))) {
            assertThat(archivos.count()).isEqualTo(1);
        }
        atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.EN_ENTREGA, null), encargado);
        var finalizado = atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.FINALIZADO, null), encargado);
        assertThat(finalizado.estadoPago()).isEqualTo("PAGADO");
        assertThat(finalizado.fechaEntrega()).isNotNull();
        assertThat(atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.FINALIZADO, null), encargado).fechaEntrega())
                .isEqualTo(finalizado.fechaEntrega());
        assertThat(repositorio.existsByIdReservaAndEstadoPedidoIn(100L, List.of(EstadoPedido.NUEVO, EstadoPedido.EN_PREPARACION, EstadoPedido.EN_ENTREGA))).isFalse();
    }

    @Test
    void rechazoExigeMotivoYCierraPedidoSinCobro() {
        var id = pedidos.crear(solicitud(UUID.randomUUID()), cliente).pedido().idPedido();
        assertThatThrownBy(() -> atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.RECHAZADO, " "), encargado))
                .isInstanceOf(ResponseStatusException.class);
        var rechazado = atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.RECHAZADO, "Sin ingredientes"), encargado);
        assertThat(rechazado.estadoPago()).isEqualTo("PENDIENTE");
        assertThat(rechazado.motivoRechazo()).isEqualTo("Sin ingredientes");
        assertThatThrownBy(() -> atencion.descargarComanda(local, id, encargado)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.EN_PREPARACION, null), encargado))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void impideProductosAgotadosOtrosLocalesYOtrosRoles() {
        gestion.cambiarDisponibilidad(local, producto, new DisponibilidadDatos(false), encargado);
        assertThatThrownBy(() -> pedidos.crear(solicitud(UUID.randomUUID()), cliente)).isInstanceOf(ResponseStatusException.class);
        var otro = gestion.crearRestaurante(new RestauranteDatos("Otro local", null, null), admin).idRestaurante();
        assertThatThrownBy(() -> panel.consultar(otro, null, null, 0, 20, encargado)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gestion.crearRestaurante(new RestauranteDatos("No permitido", null, null), cliente))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gestion.guardarProducto(otro, null, new ProductoDatos(categoria, "Incorrecto", null,
                BigDecimal.TEN, 20, true, null), admin)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gestion.eliminarCategoria(local, categoria, admin)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void fotoSeActualizaYSeRetiraSinDejarElArchivoAnterior() throws Exception {
        var imagen = new java.awt.image.BufferedImage(3, 3, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var salida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagen, "png", salida);
        var archivo = new org.springframework.mock.web.MockMultipartFile("file", "foto.png", "image/png", salida.toByteArray());
        assertThat(fotos.guardar("locales", local, archivo, admin)).contains("/imagenes/locales/");
        assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(fotos.leer("locales", local)))).isNotNull();
        fotos.guardar("locales", local, archivo, admin);
        try (var guardadas = Files.list(ARCHIVOS.resolve("locales"))) {
            assertThat(guardadas.count()).isEqualTo(1);
        }
        fotos.quitar("locales", local, admin);
        assertThatThrownBy(() -> fotos.leer("locales", local)).isInstanceOf(ResponseStatusException.class);
        try (var guardadas = Files.list(ARCHIVOS.resolve("locales"))) {
            assertThat(guardadas.count()).isZero();
        }
        assertThatThrownBy(() -> fotos.guardar("productos", producto, archivo, encargado)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void restauranteInactivoConservaAccesoOperativoPeroNoRecibePedidosNuevos() {
        var id = pedidos.crear(solicitud(UUID.randomUUID()), cliente).pedido().idPedido();
        gestion.cambiarEstado(local, new EstadoDatos(EstadoRestaurante.INACTIVO), admin);
        assertThat(consultas.consultarMiLocal(7L, encargado).estado()).isEqualTo("INACTIVO");
        assertThat(panel.consultar(local, null, null, 0, 20, encargado).totalElementos()).isEqualTo(1);
        assertThatThrownBy(() -> pedidos.crear(solicitud(UUID.randomUUID()), cliente)).isInstanceOf(ResponseStatusException.class);
        atencion.cambiarEstado(local, id, new EstadoPedidoRequest(EstadoPedido.RECHAZADO, "Local cerrado"), encargado);
    }

    @Test
    void respetaAperturaYCierrePeruanos() {
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-28T03:00:00Z"));
        assertThatThrownBy(() -> pedidos.crear(solicitud(UUID.randomUUID()), cliente)).isInstanceOf(ResponseStatusException.class);
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-27T13:00:00Z"));
        assertThat(pedidos.crear(solicitud(UUID.randomUUID()), cliente).creado()).isTrue();
    }
}
