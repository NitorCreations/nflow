package io.nflow.rest.config;

import tools.jackson.databind.ObjectMapper;
import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.EngineConfiguration.EngineObjectMapperSupplier;
import io.nflow.engine.config.NFlow;
import jakarta.inject.Named;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

@Configuration
@Import({ EngineConfiguration.class, NflowRestApiPropertiesConfiguration.class })
@ComponentScan("io.nflow.rest")
public class RestConfiguration {

  public static final String REST_OBJECT_MAPPER = "nflowRestObjectMapper";

  @Bean
  @Named(REST_OBJECT_MAPPER)
  public ObjectMapper nflowRestObjectMapper(@NFlow EngineObjectMapperSupplier nflowObjectMapper) {
    return nflowObjectMapper.get().rebuild()
        .configure(WRITE_DATES_AS_TIMESTAMPS, false)
        .enable(FAIL_ON_TRAILING_TOKENS)
        .build();
  }
}
