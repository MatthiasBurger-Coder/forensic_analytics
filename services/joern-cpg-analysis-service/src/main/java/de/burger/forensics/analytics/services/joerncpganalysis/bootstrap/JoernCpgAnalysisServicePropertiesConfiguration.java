package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JoernCpgAnalysisServiceProperties.class)
public class JoernCpgAnalysisServicePropertiesConfiguration {
}
