package io.nflow.engine.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.config.db.DatabaseConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.concurrent.ThreadFactory;

@Component
public class SpringNFlowConfiguration implements NFlowConfiguration {
    
    private final Environment env;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final ThreadFactory threadFactory;
    private final BeanFactory appCtx;

    @Inject
    SpringNFlowConfiguration(Environment env,@NFlow DataSource dataSource,
    @NFlow ObjectMapper objectMapper,
    PlatformTransactionManager transactionManager, @NFlow ThreadFactory threadFactory,
    BeanFactory appCtx) {
      this.env = env;
      this.dataSource = dataSource;
      this.objectMapper = objectMapper;
      this.transactionManager = transactionManager;
      this.threadFactory = threadFactory;
      this.appCtx = appCtx;
    }

    @Override
    public Object getMetricsRegistry() {
      Object metricRegistry = null;
      try {
        Class<?> metricClass = Class.forName("com.codahale.metrics.MetricRegistry");
        return appCtx.getBean(metricClass);
      } catch (@SuppressWarnings("unused") ClassNotFoundException | NoSuchBeanDefinitionException e) {
        // ignored - metrics is an optional dependency
        return null;
      }
    }

    @Override
    public DatabaseConfiguration getDatabaseConfiguration() {
        return null;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getProperty(String name) {
        return env.getProperty(name);
      }

    @Override
    public ObjectMapper getObjectMapper() {
      return objectMapper;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
      return transactionManager;
    }

    @Override
    public ThreadFactory getThreadFactory() {
      return threadFactory;
    }
  
}
