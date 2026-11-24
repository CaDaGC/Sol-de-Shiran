package pe.edu.upeu.soldeshiran.almacenamiento;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.IOException;
import java.security.MessageDigest;

public class AlmacenamientoService {
    private final String proveedor;
    private final LocalStorage local;
    private final DriveStorage drive;
    public AlmacenamientoService(String proveedor, LocalStorage local, DriveStorage drive) {
        if (!"local".equals(proveedor) && !"drive".equals(proveedor)) throw new IllegalArgumentException("Selecciona archivos.proveedor local o drive.");
        this.proveedor = proveedor; this.local = local; this.drive = drive;
    }
    public boolean usaDrive() { return "drive".equals(proveedor); }
    public String guardar(CategoriaArchivo categoria, String nombre, byte[] contenido, String tipo) {
        try {
            String referencia = usaDrive() ? drive.guardar(categoria, nombre, contenido, tipo) : local.guardar(categoria, nombre, contenido);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int estado) {
                        if (estado == STATUS_ROLLED_BACK) descartar(categoria, referencia);
                    }
                });
            }
            return referencia;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw error(); }
        catch (IOException e) { throw error(); }
    }
    public byte[] leer(CategoriaArchivo categoria, String referencia) {
        try {
            if (referencia != null && referencia.startsWith("drive:")) return drive.leer(categoria, referencia.substring(6));
            return local.leer(categoria, referencia != null && referencia.startsWith("local:") ? referencia.substring(6) : referencia);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw error(); }
        catch (IOException | IllegalArgumentException e) { throw error(); }
    }
    public String migrar(CategoriaArchivo categoria, String nombre, byte[] contenido, String tipo) {
        if (!usaDrive()) throw new IllegalStateException("La migración necesita archivos.proveedor=drive.");
        String referencia = guardar(categoria, nombre, contenido, tipo);
        if (!MessageDigest.isEqual(contenido, leer(categoria, referencia))) {
            descartar(categoria, referencia);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La copia de Drive no coincide con el archivo original. Se conservó el original.");
        }
        return referencia;
    }
    public void descartar(CategoriaArchivo categoria, String referencia) {
        if (referencia == null) return;
        try {
            if (referencia.startsWith("drive:")) drive.descartar(referencia.substring(6));
            else local.descartar(categoria, referencia.startsWith("local:") ? referencia.substring(6) : referencia);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (IOException | RuntimeException e) { org.slf4j.LoggerFactory.getLogger(AlmacenamientoService.class).warn("No se pudo retirar un archivo no confirmado del almacenamiento."); }
    }
    private static ResponseStatusException error() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo acceder al almacenamiento de archivos. Reintenta la operación."); }
}
