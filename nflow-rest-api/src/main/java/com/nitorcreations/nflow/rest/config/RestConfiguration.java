package com.nitorcreations.nflow.rest.config;

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

  @Bean(name="nflow-rest-ObjectMapper")
  public ObjectMapper humanObjectMapper(@Named("nflow-ObjectMapper") ObjectMapper engineObjectMapper) {
    ObjectMapper restObjectMapper = engineObjectMapper.copy();
    restObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return restObjectMapper;
  }

}
