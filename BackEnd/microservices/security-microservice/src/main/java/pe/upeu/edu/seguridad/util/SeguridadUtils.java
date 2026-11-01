package pe.upeu.edu.seguridad.util;

import pe.upeu.edu.seguridad.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class SeguridadUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SeguridadUtils() {}

    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String codigoRecuperacion() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String email(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static void password(String value) {
        if (value == null
                || value.length() < 12
                || value.getBytes(StandardCharsets.UTF_8).length > 72
                || value.isBlank())
            throw ApiException.invalido(
                    "La contraseña debe tener al menos 12 caracteres y un máximo de 72 bytes"
                            + " UTF-8.");
    }
}
