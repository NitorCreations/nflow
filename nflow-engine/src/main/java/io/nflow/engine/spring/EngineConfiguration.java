package io.nflow.engine.spring;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.lang.Runtime.getRuntime;

import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;

/**
 * The main Spring configuration class for nFlow engine.
 */
@Configuration
@ComponentScan("io.nflow.engine")
public class EngineConfiguration {

  /**
   * Creates a thread factory for creating instance executor threads.
   * @return Instance executor thread factory.
   */
  @Bean
  @NFlow
  public ThreadFactory nflowThreadFactory() {
    CustomizableThreadFactory factory = new CustomizableThreadFactory("nflow-executor-");
    factory.setThreadGroupName("nflow");
    return factory;
  }

  /**
   * Creates an object mapper for serializing and deserializing workflow instance state variables to and from database.
   * @return Object mapper.
   */
  @Bean
  @NFlow
  public ObjectMapper nflowObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
    return mapper;
  }

  /**
   * Creates a resource for listing workflows that are not defined as Spring beans.
   * @param env The Spring environment.
   * @return A resource representing the file that contains a list of workflow class names.
   */
  @Bean
  @NFlow
  public AbstractResource nflowNonSpringWorkflowsListing(Environment env) {
    String filename = env.getProperty("nflow.non_spring_workflows_filename");
    if (filename != null) {
      return new ClassPathResource(filename);
    }
    return new ByteArrayResource(new byte[0]);
  }
}
