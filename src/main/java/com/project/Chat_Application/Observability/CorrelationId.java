package com.project.Chat_Application.Observability;

import org.slf4j.MDC;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final int MAX_LENGTH = 64;

    private CorrelationId() {}

    public static String generate() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // Cap length so a client can't write huge or malicious values into the logs
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return generate();
        }
        String trimmed = raw.trim();
        return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static String orNone() {
        String id = current();
        return id != null ? id : "none";
    }
}