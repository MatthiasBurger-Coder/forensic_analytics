package de.burger.forensics.analytics.services.cliclient.adapter.out.http;

public final class PublicApiClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    PublicApiClientException(String message) {
        super(message);
    }
}
