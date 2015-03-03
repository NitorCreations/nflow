package com.nitorcreations.nflow.engine.internal.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.lang.Runtime.getRuntime;

import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.nitorcreations.nflow.engine.internal.executor.WorkflowInstanceExecutor;

@Configuration
@ComponentScan("com.nitorcreations.nflow.engine")
public class EngineConfiguration {

  @Bean
  public WorkflowInstanceExecutor nflowExecutor(@NFlow ThreadFactory nflowThreadFactory, Environment env) {
    int threadCount = env.getProperty("nflow.executor.thread.count", Integer.class, 2 * getRuntime().availableProcessors());
    int awaitTerminationSeconds = env.getRequiredProperty("nflow.dispatcher.await.termination.seconds", Integer.class);
    int queueSize = env.getProperty("nflow.dispatcher.executor.queue.size", Integer.class, 2 * threadCount);
    int notifyThreshold = env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, queueSize / 2);
    int keepAliveSeconds = env.getRequiredProperty("nflow.dispatcher.executor.thread.keepalive.seconds", Integer.class);
    return new WorkflowInstanceExecutor(queueSize, threadCount, notifyThreshold, awaitTerminationSeconds, keepAliveSeconds,
        nflowThreadFactory);
  }

  @Bean
  @NFlow
  public ThreadFactory nflowThreadFactory() {
    CustomizableThreadFactory factory = new CustomizableThreadFactory("nflow-executor-");
    factory.setThreadGroupName("nflow");
    return factory;
  }

  @Bean
  @NFlow
  public ObjectMapper nflowObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
    return mapper;
  }

  @Bean
  @NFlow
  public AbstractResource nflowNonSpringWorkflowsListing(Environment env) {
    String filename = env.getProperty("nflow.non_spring_workflows_filename");
    if (filename != null) {
      return new ClassPathResource(filename);
    }
    return null;
  }
}
