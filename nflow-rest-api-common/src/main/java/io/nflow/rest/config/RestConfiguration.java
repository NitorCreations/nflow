package io.nflow.rest.config;

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.EngineConfiguration.EngineJsonMapperSupplier;
import io.nflow.engine.config.NFlow;
import jakarta.inject.Named;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Import({ EngineConfiguration.class, NflowRestApiPropertiesConfiguration.class })
@ComponentScan("io.nflow.rest")
public class RestConfiguration {

  public static final String REST_JSON_MAPPER = "nflowRestJsonMapper";

  @Bean
  @Named(REST_JSON_MAPPER)
  public JsonMapper nflowRestJsonMapper(@NFlow EngineJsonMapperSupplier nflowJsonMapper) {
    return nflowJsonMapper.get().rebuild()
        .configure(WRITE_DATES_AS_TIMESTAMPS, false)
        .enable(FAIL_ON_TRAILING_TOKENS)
        .build();
  }
}
