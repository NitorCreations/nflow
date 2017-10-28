package io.nflow.netty.config;

import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;

import io.nflow.engine.config.NFlow;
import io.nflow.rest.config.RestConfiguration;

@Configuration
@ComponentScan("ios.nflow.rest.v1.springweb")
@Import(RestConfiguration.class)
@EnableTransactionManagement
@EnableWebFlux
public class NflowNettyConfiguration {

  @Bean
  public PlatformTransactionManager transactionManager(@NFlow DataSource nflowDataSource) {
    return new DataSourceTransactionManager(nflowDataSource);
  }

  @Bean
  public DispatcherHandler webHandler(ApplicationContext context) {
    return new DispatcherHandler(context);
  }

}
