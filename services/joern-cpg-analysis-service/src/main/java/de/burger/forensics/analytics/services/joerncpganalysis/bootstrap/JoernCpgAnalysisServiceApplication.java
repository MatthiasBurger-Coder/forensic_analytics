package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

@SpringBootApplication
@EnableConfigurationProperties(JoernCpgAnalysisServiceProperties.class)
public class JoernCpgAnalysisServiceApplication {
    public static void main(String[] args) {
        run(args, applicationArgs -> SpringApplication.run(JoernCpgAnalysisServiceApplication.class, applicationArgs), System::exit);
    }

    static void run(String[] args, Consumer<String[]> starter, IntConsumer exit) {
        if (HealthProbe.isHealthCheck(args)) {
            exit.accept(HealthProbe.run(args));
            return;
        }
        starter.accept(args);
    }
}
