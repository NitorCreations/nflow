package io.nflow.rest.config;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import javax.inject.Named;

import org.joda.time.ReadablePeriod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.deser.PeriodDeserializer;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;

@Configuration
@Import({ EngineConfiguration.class, NflowRestApiPropertiesConfiguration.class })
@ComponentScan("io.nflow.rest")
public class RestConfiguration {

  public static final String REST_OBJECT_MAPPER = "nflowRestObjectMapper";

  @Bean
  @Primary // Needed to get spring-boot to find the right mapper by default for Jackson
  @Named(REST_OBJECT_MAPPER)
  public ObjectMapper nflowRestObjectMapper(@NFlow ObjectMapper nflowObjectMapper) {
    ObjectMapper restObjectMapper = nflowObjectMapper.copy();
    restObjectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(ReadablePeriod.class, new PeriodDeserializer(false));
    restObjectMapper.registerModule(module);
    return restObjectMapper;
  }
}
