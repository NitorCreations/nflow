package io.nflow.springboot.bareminimum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.nflow.engine.config.EngineConfiguration;

@SpringBootApplication
@Import(EngineConfiguration.class)
public class SpringBootBareMinimumApplication {

  @Bean
  public ExampleWorkflow exampleWorkflow() {
    return new ExampleWorkflow();
  }

  public static void main(String[] args) {
    SpringApplication.run(SpringBootBareMinimumApplication.class, args);
  }

}
