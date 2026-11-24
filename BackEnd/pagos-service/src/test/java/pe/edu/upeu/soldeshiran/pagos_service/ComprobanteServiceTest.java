package pe.edu.upeu.soldeshiran.pagos_service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.service.impl.ComprobanteServiceImpl;
import pe.edu.upeu.soldeshiran.pagos_service.service.ComprobantePdf;
import pe.edu.upeu.soldeshiran.pagos_service.mapper.ComprobanteMapper;
import pe.edu.upeu.soldeshiran.pagos_service.repository.*;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.DatosReservaComprobante;
import pe.edu.upeu.soldeshiran.pagos_service.model.*;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
class ComprobanteServiceTest {
  @TempDir Path carpeta;
  ComprobantePagoRepository repo;TransaccionPagoRepository pagos;ReservaInternaClient reservas;
  ComprobanteServiceImpl service;ObjectMapper mapper;com.sun.net.httpserver.HttpServer usuarios;
  TransaccionPago tx;DatosReservaComprobante reserva;
  @BeforeEach void setup() throws Exception {
    repo=mock(ComprobantePagoRepository.class);pagos=mock(TransaccionPagoRepository.class);reservas=mock(ReservaInternaClient.class);
    mapper=new ObjectMapper().findAndRegisterModules();
    service=new ComprobanteServiceImpl(repo,pagos,reservas,mapper,new ComprobantePdf(),new ComprobanteMapper(), new pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService("local", new pe.edu.upeu.soldeshiran.almacenamiento.LocalStorage(Map.of(pe.edu.upeu.soldeshiran.almacenamiento.CategoriaArchivo.COMPROBANTE, carpeta)), null));
    ReflectionTestUtils.setField(service,"token","interno");ReflectionTestUtils.setField(service,"key","clave");
    usuarios=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
    usuarios.createContext("/internal/usuarios/5/perfil",exchange->{
      assertEquals("clave",exchange.getRequestHeaders().getFirst("X-Internal-Key"));
      byte[] body="""
        {"nombreUsuario":"alias","email":"cliente@example.com","persona":{"nombres":"Carlos Daniel","apellidos":"Gonzales Chilón","tipoDocumento":"DNI","numeroDocumento":"12345678","telefono":"999888777","direccion":"Dirección del cliente"}}
        """.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});usuarios.start();
    ReflectionTestUtils.setField(service,"usuariosUrl","http://127.0.0.1:"+usuarios.getAddress().getPort());
    tx=new TransaccionPago();tx.setIdTransaccion(7L);tx.setIdReserva(9L);tx.setEstadoTransaccion(EstadoTransaccion.APROBADA);tx.setMonto(new BigDecimal("125.00"));tx.setIdExternoPasarela("MP-TEST");tx.setFechaTransaccion(LocalDateTime.of(2026,11,20,12,0));
    reserva=new DatosReservaComprobante(9L,5L,"RSS00009","Cabaña Familiar",LocalDate.of(2026,11,21),LocalDate.of(2026,11,22),new BigDecimal("250.00"),"CONFIRMADA");
    when(pagos.bloquearComprobante(7L)).thenReturn(Optional.of(tx));when(reservas.datosComprobante(9L,"interno")).thenReturn(reserva);
    when(pagos.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(9L,TransaccionPago.ONLINE_ADELANTO,EstadoTransaccion.APROBADA)).thenReturn(Optional.of(tx));
    when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
  }
  @AfterEach void cerrar(){usuarios.stop(0);}
  @Test void generaPdfRealConDatosDelClienteYConservaLaCopia() throws Exception {
    var c=service.consultar(9L,new Actor(5L,"CLIENTE"));byte[] bytes=service.pdf(c);
    assertTrue(bytes.length>1000);assertTrue(Files.exists(carpeta.resolve(c.getArchivo())));
    assertTrue(c.getDatosJson().contains("Gonzales Chilón"));assertEquals(5L,c.getIdUsuario());
    try(var doc=org.apache.pdfbox.Loader.loadPDF(bytes)){
      String texto=new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
      assertTrue(texto.contains("Carlos Daniel"));assertTrue(texto.contains("RSS00009"));assertTrue(texto.contains("125.00"));assertTrue(texto.contains("Luis Pantoja"));assertFalse(texto.contains("RUC"));
      var destino=System.getenv("COMPROBANTE_PREVIEW_DIR");if(destino!=null){Files.createDirectories(Path.of(destino));Files.write(Path.of(destino,"comprobante-ejemplo.pdf"),bytes);javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0,120),"png",Path.of(destino,"comprobante-ejemplo.png").toFile());}
    }
    when(repo.findByIdReserva(9L)).thenReturn(Optional.of(c));reset(reservas);
    var otra=service.consultar(9L,new Actor(5L,"CLIENTE"));assertArrayEquals(bytes,service.pdf(otra));verifyNoInteractions(reservas);verify(repo,times(1)).saveAndFlush(any());
  }
  @Test void clienteNoPuedeObtenerComprobanteAjeno(){
    assertEquals(404,assertThrows(ResponseStatusException.class,()->service.consultar(9L,new Actor(8L,"CLIENTE"))).getStatusCode().value());
    verify(repo,never()).saveAndFlush(any());
  }
  @Test void noGeneraParaRestanteDanosNiPagoPendiente(){
    for(String tipo:List.of(TransaccionPago.RESTANTE,TransaccionPago.DANOS)){
      tx.setTipoPago(tipo);assertThrows(ResponseStatusException.class,()->service.emitir(7L));
    }
    tx.setTipoPago(TransaccionPago.ONLINE_ADELANTO);tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
    assertThrows(ResponseStatusException.class,()->service.emitir(7L));verify(repo,never()).saveAndFlush(any());
  }
  @Test void personalSoloAccedeDespuesDelCheckIn(){
    for(String rol:List.of("ADMIN","TRABAJADOR")) assertEquals(403,assertThrows(ResponseStatusException.class,()->service.consultar(9L,new Actor(2L,rol))).getStatusCode().value());
    when(reservas.datosComprobante(9L,"interno")).thenReturn(new DatosReservaComprobante(reserva.idReserva(),reserva.idUsuario(),reserva.codigo(),reserva.nombreCabana(),reserva.fechaIngreso(),reserva.fechaSalida(),reserva.montoTotal(),"OCUPADA"));
    var c=service.consultar(9L,new Actor(2L,"TRABAJADOR"));assertEquals(5L,c.getIdUsuario());
  }
}
