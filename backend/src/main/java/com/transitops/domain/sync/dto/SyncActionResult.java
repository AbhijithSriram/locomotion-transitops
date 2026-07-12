package com.transitops.domain.sync.dto;

public record SyncActionResult(
        String idempotencyKey,
        String result, // "applied" | "conflict" | "error"
        String message
) {
    public static SyncActionResult applied(String key) {
        return new SyncActionResult(key, "applied", null);
    }
    public static SyncActionResult conflict(String key, String message) {
        return new SyncActionResult(key, "conflict", message);
    }
    public static SyncActionResult error(String key, String message) {
        return new SyncActionResult(key, "error", message);
    }
}