package com.aistudio.infrastructure.auth;

final class SamlPemUtils {

    private SamlPemUtils() {
    }

    static String normalizePrivateKey(String pem) {
        return stripPemHeaders(pem, "PRIVATE KEY", "RSA PRIVATE KEY");
    }

    static String normalizeCertificate(String pem) {
        return stripPemHeaders(pem, "CERTIFICATE");
    }

    private static String stripPemHeaders(String pem, String... labels) {
        if (pem == null || pem.isBlank()) {
            return "";
        }
        String normalized = pem.trim();
        for (String label : labels) {
            normalized = normalized.replace("-----BEGIN " + label + "-----", "");
            normalized = normalized.replace("-----END " + label + "-----", "");
        }
        return normalized.replaceAll("\\s+", "");
    }
}
