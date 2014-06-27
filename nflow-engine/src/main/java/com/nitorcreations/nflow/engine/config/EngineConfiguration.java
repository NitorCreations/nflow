package com.nitorcreations.nflow.engine.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ComponentScan("com.nitorcreations.nflow.engine")
public class EngineConfiguration {

  @Inject
  Environment env;

  @Bean(name="nflow-executor")
  public ThreadPoolTaskExecutor dispatcherPoolExecutor(@Named("nflow-ThreadFactory") ThreadFactory threadFactory) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    Integer threadCount = env.getProperty("nflow.executor.thread.count", Integer.class, 2 * Runtime.getRuntime().availableProcessors());
    executor.setCorePoolSize(threadCount);
    executor.setMaxPoolSize(threadCount);
    executor.setKeepAliveSeconds(0);
    executor.setAwaitTerminationSeconds(60);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadFactory(threadFactory);
    return executor;
  }

  @Bean(name="nflow-ThreadFactory")
  public ThreadFactory threadFactory() {
    CustomizableThreadFactory factory = new CustomizableThreadFactory("nflow-executor-");
    factory.setThreadGroupName("nflow");
    return factory;
  }

  @Bean(name="nflow-ObjectMapper")
  public ObjectMapper nflowObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    return mapper;
  }

  @Bean(name = "non-spring-workflows-listing")
  public AbstractResource nonSpringWorkflowsListing() {
    String filename = env.getProperty("nflow.non_spring_workflows_filename");
    if (filename != null) {
      return new ClassPathResource(filename);
    }
    return null;
  }

}
