package pe.edu.upeu.soldeshiran.restaurante_service.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

@Component
public class ComandaPdf {

    public byte[] generar(Pedido pedido) throws IOException {
        var lineas = new ArrayList<String>();

        lineas.add("COMPLEJO TURISTICO SOL DE SHIRAN");
        lineas.add("COMANDA " + pedido.getCodigoPedido());
        lineas.add("Restaurante: " + pedido.getRestaurante().getNombre());
        lineas.add("Cabana: " + pedido.getNombreCabana());
        lineas.add("Huesped: " + pedido.getNombreHuesped());
        lineas.add("Reserva ID: " + pedido.getIdReserva());
        lineas.add("Pedido: " + pedido.getFechaPedido().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        lineas.add("Aceptado: " + pedido.getFechaAceptacion().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        lineas.add("");
        lineas.add("PRODUCTOS");

        for (var detalle : pedido.getDetalles()) {
            lineas.add(
                    detalle.getCantidad()
                            + " x "
                            + detalle.getNombreProducto()
            );

            lineas.add(
                    "Precio: S/ "
                            + detalle.getPrecioUnitario().toPlainString()
                            + " | Subtotal: S/ "
                            + detalle.getSubtotal().toPlainString()
            );

            if (detalle.getObservaciones() != null
                    && !detalle.getObservaciones().isBlank()) {

                lineas.add(
                        "OBSERVACIONES: " + detalle.getObservaciones()
                );
            }

            lineas.add("");
        }

        lineas.add("TOTAL A COBRAR: S/ "
                + pedido.getMontoTotal().toPlainString());

        lineas.add("Metodo presencial: "
                + pedido.getMetodoPagoPresencial().name());

        lineas.add("PAGO PENDIENTE DE COBRO");
        lineas.add("Documento operativo para cocina y reparto.");
        lineas.add("No acredita el pago del pedido.");

        try (var documento = new PDDocument();
             var salida = new ByteArrayOutputStream()) {

            PDFont fuente = new PDType1Font(
                    Standard14Fonts.FontName.COURIER
            );

            var texto = preparar(lineas, fuente);

            for (int inicio = 0; inicio < texto.size(); inicio += 48) {
                var pagina = new PDPage();
                documento.addPage(pagina);

                try (var contenido = new PDPageContentStream(
                        documento,
                        pagina
                )) {
                    contenido.beginText();
                    contenido.setFont(fuente, 10);
                    contenido.setLeading(14);
                    contenido.newLineAtOffset(40, 750);

                    int fin = Math.min(inicio + 48, texto.size());

                    for (int posicion = inicio; posicion < fin; posicion++) {
                        contenido.showText(texto.get(posicion));
                        contenido.newLine();
                    }

                    contenido.endText();
                }
            }

            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private List<String> preparar(
            List<String> lineas,
            PDFont fuente) throws IOException {

        var resultado = new ArrayList<String>();

        for (String linea : lineas) {
            var limpia = new StringBuilder();

            for (int punto : linea.codePoints().toArray()) {
                String caracter = Character.isISOControl(punto)
                        ? " "
                        : new String(Character.toChars(punto));

                try {
                    fuente.encode(caracter);
                } catch (IllegalArgumentException exception) {
                    caracter = "?";
                }

                limpia.append(caracter);
            }

            if (limpia.isEmpty()) {
                resultado.add("");
                continue;
            }

            for (int inicio = 0; inicio < limpia.length(); inicio += 84) {
                resultado.add(limpia.substring(
                        inicio,
                        Math.min(inicio + 84, limpia.length())
                ));
            }
        }

        return resultado;
    }
}