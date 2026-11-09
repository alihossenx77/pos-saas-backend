package com.pos.saas.payload.response;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Common wrapper for EVERY response (success and error).
 *
 * { "success": true, "status": 200, "message": "...", "data": {...}, "errors": null, "timestamp": "..." }
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final T data;
    private final Map<String, String> errors;   // field -> message (validation errors only)
    private final String timestamp;

    private ApiResponse(boolean success, int status, String message, T data, Map<String, String> errors) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = Instant.now().toString();
    }

    // ---- success ----

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, "Success", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, null);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, 201, message, data, null);
    }

    /** Success with no data (e.g. delete). */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, 200, message, null, null);
    }

    // ---- error ----

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, message, null, null);
    }

    public static <T> ApiResponse<T> error(int status, String message, Map<String, String> errors) {
        return new ApiResponse<>(false, status, message, null, errors);
    }
}
