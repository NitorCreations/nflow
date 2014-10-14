package com.nitorcreations.nflow.rest.config;

import static com.nitorcreations.nflow.engine.internal.config.EngineConfiguration.NFLOW_OBJECT_MAPPER;

import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nitorcreations.nflow.engine.internal.config.EngineConfiguration;

@Configuration
@Import(EngineConfiguration.class)
@ComponentScan("com.nitorcreations.nflow.rest")
public class RestConfiguration {

  public static final String NFLOW_REST_OBJECT_MAPPER = "nflowRestObjectMapper";

  @Bean(name = NFLOW_REST_OBJECT_MAPPER)
  public ObjectMapper humanObjectMapper(@Named(NFLOW_OBJECT_MAPPER) ObjectMapper engineObjectMapper) {
    ObjectMapper restObjectMapper = engineObjectMapper.copy();
    restObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return restObjectMapper;
  }

}
