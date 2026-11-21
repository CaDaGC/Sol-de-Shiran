package pe.edu.upeu.soldeshiran.pagos_service.service;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.pagos_service.dto.ComprobanteDatos;
import java.io.*;
import java.util.*;
@Component
public class ComprobantePdf {
  public byte[] generar(ComprobanteDatos d) throws IOException {
    try(var doc=new PDDocument(); var fuente=new ClassPathResource("comprobantes/Lato-Regular.ttf").getInputStream()) {
      var font=PDType0Font.load(doc,fuente);var page=new PDPage();doc.addPage(page);
      try(var out=new PDPageContentStream(doc,page)) {
        out.setNonStrokingColor(30/255f,62/255f,46/255f);out.addRect(0,660,612,132);out.fill();
        var logo=new ClassPathResource("comprobantes/logo.png").getInputStream();
        try(logo){var img=PDImageXObject.createFromByteArray(doc,logo.readAllBytes(),"logo");out.drawImage(img,440,698,125,68);}
        texto(out,font,42,749,16,"COMPLEJO TURÍSTICO");texto(out,font,42,727,20,"SOL DE SHIRAN");
        texto(out,font,42,700,10,"Comprobante de pago del adelanto");
        out.setNonStrokingColor(30/255f,62/255f,46/255f);
        var lineas=new ArrayList<String>();
        lineas.add("COMPROBANTE: "+d.numero()+"     Emisión: "+fecha(d.emision()));
        lineas.add("Dirección: A LA ALTURA DE LA TRANCA, 13125, CARRETERA CENTRAL KM 39, Shiran 13125");
        lineas.add("Contacto: +51 964 200 983 - Luis Pantoja");lineas.add("");
        lineas.add("DATOS DEL CLIENTE");lineas.add("Cliente: "+d.cliente());
        lineas.add("Documento: "+d.tipoDocumento()+" "+d.documento());lineas.add("Correo: "+d.email());
        lineas.add("Teléfono: "+d.telefono());lineas.add("Dirección: "+d.direccion());lineas.add("");
        lineas.add("RESERVA "+d.codigoReserva());lineas.add("Cabaña: "+d.cabana());
        lineas.add("Ingreso: "+d.ingreso()+" 11:00 a. m.     Salida: "+d.salida()+" 1:00 p. m.");lineas.add("");
        lineas.add("PAGO APROBADO - MERCADO PAGO");lineas.add("Concepto: adelanto de reserva");
        lineas.add("Transacción: "+d.transaccion()+"     Referencia: "+d.referencia());
        lineas.add("Fecha de operación: "+fecha(d.fechaPago()));
        lineas.add("IMPORTE RECIBIDO: "+d.moneda()+" "+d.importe().toPlainString());
        lineas.add("Total de la reserva: "+d.moneda()+" "+d.totalReserva().toPlainString());
        lineas.add("Saldo tras el adelanto: "+d.moneda()+" "+d.saldoTrasAdelanto().toPlainString());lineas.add("");
        lineas.add("Este comprobante corresponde únicamente al adelanto pagado mediante Mercado Pago.");
        lineas.add("No acredita pagos restantes ni pagos por daños. No es una boleta ni factura electrónica.");
        float y=637;
        for(String linea:lineas) {
          String limpia=linea.replaceAll("[\\p{Cntrl}]"," ");
          for(String trozo:envolver(limpia,font,10,525)) {
            if(y<35) throw new IOException("El contenido excede el formato del comprobante");
            out.beginText();out.setFont(font,10);out.newLineAtOffset(42,y);out.showText(trozo);out.endText();y-=15;
          }
        }
      }
      var salida=new ByteArrayOutputStream();doc.save(salida);return salida.toByteArray();
    }
  }
  private void texto(PDPageContentStream out,PDFont f,float x,float y,float size,String text)throws IOException{
    out.setNonStrokingColor(1f,1f,1f);out.beginText();out.setFont(f,size);out.newLineAtOffset(x,y);out.showText(text);out.endText();
  }
  private java.util.List<String> envolver(String texto,PDFont f,int size,int max)throws IOException {
    var r=new ArrayList<String>();var actual=new StringBuilder();
    for(int i=0;i<texto.length();){int punto=texto.codePointAt(i);String c=new String(Character.toChars(punto));i+=Character.charCount(punto);
      try{f.encode(c);}catch(IllegalArgumentException e){c="?";}
      if(f.getStringWidth(actual.toString()+c)/1000*size>max){r.add(actual.toString());actual.setLength(0);}actual.append(c);
    }r.add(actual.toString());return r;
  }
  private String fecha(java.time.LocalDateTime f){return f==null?"No registrada":f.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
}
