package de.burger.forensics.analytics.services.analysisstore.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AnalysisStoreServiceApplication {
    public static void main(String[] args) {
        if (HealthProbe.isHealthCheck(args)) {
            System.exit(HealthProbe.run(args));
        }
        run(args);
    }

    static ConfigurableApplicationContext run(String[] args) {
        return SpringApplication.run(AnalysisStoreServiceApplication.class, args);
    }
}
