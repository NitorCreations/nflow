package io.nflow.rest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import jakarta.inject.Named;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * This configuration configures default ObjectMapper to use. If you don't want to use it
 * you can import {@link EngineConfiguration} and define primary ObjectMapper nflowRestObjectMapper bean.
 */
@Configuration
@Import({ EngineConfiguration.class, NflowRestApiPropertiesConfiguration.class })
@ComponentScan("io.nflow.rest")
public class RestConfiguration {

  public static final String REST_OBJECT_MAPPER = "nflowRestObjectMapper";

  @Primary
  @Bean
  @Named(REST_OBJECT_MAPPER)
  public ObjectMapper nflowRestObjectMapper(@NFlow ObjectMapper nflowObjectMapper) {
    ObjectMapper restObjectMapper = nflowObjectMapper.copy();
    restObjectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    restObjectMapper.enable(FAIL_ON_TRAILING_TOKENS);
    return restObjectMapper;
  }
}
