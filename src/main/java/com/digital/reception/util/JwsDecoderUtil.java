package com.digital.reception.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwsDecoderUtil {

    private JwsDecoderUtil() {
    }

    public static String decodePayload(String jws) {
        if (jws == null || jws.isBlank()) {
            throw new IllegalArgumentException("El documento JWS es obligatorio");
        }
        String[] parts = jws.split("\\.", -1);
        if (parts.length != 3 || parts[1].isBlank()) {
            throw new IllegalArgumentException("El documento no tiene formato JWS compacto");
        }
        try {

            return new String(decodeP(parts), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El payload JWS no contiene Base64 válido", exception);
        }
    }

    private static byte[] decodeP(String[] parts) {
        byte[] decoded;

        try {
            decoded = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException _) {
            decoded = Base64.getDecoder().decode(parts[1]);
        }

        return decoded;
    }
}
