package com.nitorcreations.nflow.rest.config;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.internal.config.EngineConfiguration;
import com.nitorcreations.nflow.engine.internal.config.NFlow;

@Configuration
@Import(EngineConfiguration.class)
@ComponentScan("com.nitorcreations.nflow.rest")
public class RestConfiguration {

  @Bean
  @NFlowRest
  public ObjectMapper humanObjectMapper(@NFlow ObjectMapper engineObjectMapper) {
    ObjectMapper restObjectMapper = engineObjectMapper.copy();
    restObjectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    return restObjectMapper;
  }
}
