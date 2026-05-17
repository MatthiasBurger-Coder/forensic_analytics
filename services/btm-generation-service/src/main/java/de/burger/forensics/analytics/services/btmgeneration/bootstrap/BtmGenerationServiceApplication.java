package de.burger.forensics.analytics.services.btmgeneration.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

@SpringBootApplication
public class BtmGenerationServiceApplication {
    public static void main(String[] args) {
        run(args, applicationArgs -> SpringApplication.run(BtmGenerationServiceApplication.class, applicationArgs), System::exit);
    }

    static void run(String[] args, Consumer<String[]> starter, IntConsumer exit) {
        if (HealthProbe.isHealthCheck(args)) {
            exit.accept(HealthProbe.run(args));
            return;
        }
        starter.accept(args);
    }
}
