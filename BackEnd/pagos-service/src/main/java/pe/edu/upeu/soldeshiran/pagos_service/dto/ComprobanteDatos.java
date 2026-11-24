package pe.edu.upeu.soldeshiran.pagos_service.dto;
import java.math.BigDecimal;
import java.time.*;
public record ComprobanteDatos(String numero, LocalDateTime emision, Long idUsuario, String cliente,
    String tipoDocumento, String documento, String telefono, String direccion, String email,
    String codigoReserva, String cabana, LocalDate ingreso, LocalDate salida,
    Long transaccion, String referencia, LocalDateTime fechaPago, String moneda,
    BigDecimal importe, BigDecimal totalReserva, BigDecimal saldoTrasAdelanto) {}
