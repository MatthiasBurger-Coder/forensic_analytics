package de.burger.forensics.analytics.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ForensicAnalyticsApplication {
    public static void main(String[] args) {
        run(args);
    }

    static ConfigurableApplicationContext run(String[] args) {
        return SpringApplication.run(ForensicAnalyticsApplication.class, args);
    }
}
