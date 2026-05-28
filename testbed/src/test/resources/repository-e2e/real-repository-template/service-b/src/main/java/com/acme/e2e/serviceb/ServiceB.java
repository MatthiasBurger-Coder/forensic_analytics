package com.acme.e2e.serviceb;

public final class ServiceB {
    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
