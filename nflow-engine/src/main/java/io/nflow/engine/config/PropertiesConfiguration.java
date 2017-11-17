package io.nflow.engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring configuration for including properties from nflow-engine.properties file.
 */
@Configuration
@PropertySource("classpath:nflow-engine.properties")
public class PropertiesConfiguration {

}
