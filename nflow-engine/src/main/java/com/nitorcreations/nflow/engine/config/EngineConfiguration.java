package com.nitorcreations.nflow.engine.config;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan("com.nitorcreations.nflow.engine")
public class EngineConfiguration {

  @Inject
  Environment env;

  @Bean(name="nflow-executor")
  public ThreadPoolTaskExecutor dispatcherPoolExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    Integer threadCount = env.getProperty("executor.thread.count", Integer.class, 2 * Runtime.getRuntime().availableProcessors());
    executor.setCorePoolSize(threadCount);
    executor.setMaxPoolSize(threadCount);
    executor.setKeepAliveSeconds(0);
    executor.setAwaitTerminationSeconds(60);
    executor.setThreadFactory(new CustomizableThreadFactory("nflow-executor-"));
    return executor;
  }

  @Bean(name = "workflow-definition-listing")
  public ClassPathResource workflowDefinitionListing() {
    return new ClassPathResource("nflow-workflows.txt");
  }

}
