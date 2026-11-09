package com.pos.saas.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

/** Writes the ApiResponse error shape from servlet-level handlers (no Jackson needed). */
final class JsonErrorWriter {

    private JsonErrorWriter() {}

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{\"success\":false,\"status\":" + status
                + ",\"message\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
                + ",\"data\":null,\"errors\":null,\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(body);
    }
}
