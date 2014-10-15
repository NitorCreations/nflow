package com.nitorcreations.nflow.engine.internal.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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
import com.nitorcreations.nflow.engine.internal.executor.ThresholdThreadPoolTaskExecutor;

@Configuration
@ComponentScan("com.nitorcreations.nflow.engine")
public class EngineConfiguration {

  @Bean
  @NFlow
  public ThresholdThreadPoolTaskExecutor dispatcherPoolExecutor(@NFlow ThreadFactory threadFactory, Environment env) {
    ThresholdThreadPoolTaskExecutor executor = new ThresholdThreadPoolTaskExecutor();
    Integer threadCount = env.getProperty("nflow.executor.thread.count", Integer.class, 2 * Runtime.getRuntime().availableProcessors());
    executor.setCorePoolSize(threadCount);
    executor.setMaxPoolSize(threadCount);
    executor.setKeepAliveSeconds(0);
    executor.setAwaitTerminationSeconds(env.getProperty("nflow.dispatcher.await.termination.seconds", Integer.class, 60));
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setNotifyThreshold(env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, 0));
    executor.setThreadFactory(threadFactory);
    return executor;
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
  public AbstractResource nonSpringWorkflowsListing(Environment env) {
    String filename = env.getProperty("nflow.non_spring_workflows_filename");
    if (filename != null) {
      return new ClassPathResource(filename);
    }
    return null;
  }
}
