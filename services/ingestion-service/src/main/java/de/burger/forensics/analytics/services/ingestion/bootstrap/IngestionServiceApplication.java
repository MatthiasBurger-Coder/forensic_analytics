package de.burger.forensics.analytics.services.ingestion.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class IngestionServiceApplication {
    public static void main(String[] args) {
        if (HealthProbe.isHealthCheck(args)) {
            System.exit(HealthProbe.run(args));
        }
        run(args);
    }

    static ConfigurableApplicationContext run(String[] args) {
        return SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
