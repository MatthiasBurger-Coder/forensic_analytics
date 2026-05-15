package de.burger.forensics.analytics.rest;

public interface RestApiServer {
    void start();

    void stop();

    void awaitTermination() throws InterruptedException;

    int port();
}
