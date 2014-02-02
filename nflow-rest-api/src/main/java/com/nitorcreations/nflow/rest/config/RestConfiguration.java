package com.nitorcreations.nflow.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.nitorcreations.nflow.engine.config.EngineConfiguration;
import com.nitorcreations.nflow.rest.v0.WorkflowInstanceResource;

@Configuration
@Import(EngineConfiguration.class)
@ComponentScan("com.nitorcreations.nflow.rest")
public class RestConfiguration {

  @Bean
  public WorkflowInstanceResource workflowInstanceResource() {
    return new WorkflowInstanceResource();
  }
  
}
