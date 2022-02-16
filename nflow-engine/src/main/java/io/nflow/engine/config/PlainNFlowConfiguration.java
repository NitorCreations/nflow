package io.nflow.engine.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.PlatformTransactionManager;

import io.nflow.engine.internal.storage.db.SQLVariants;

public class PlainNFlowConfiguration implements NFlowConfiguration {
    
    private final Map<String, String> properties = new HashMap<>();
    private DataSource dataSource;
    private SQLVariants sqlVariant;
    private ObjectMapper objectMapper;
    private PlatformTransactionManager transactionManager;
    private Object metricsRegistry;

    public PlainNFlowConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public PlainNFlowConfiguration setSQLVariants(SQLVariants sqlVariant) {
      this.sqlVariant = sqlVariant;
      return this;
    }

    public PlainNFlowConfiguration setObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public PlainNFlowConfiguration setTransactionManager(PlatformTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
      return this;
    }

    public PlainNFlowConfiguration setMetricsRegistry(Object metricsRegistry) {
      if (!"com.codahale.metrics.MetricRegistry".equals(metricsRegistry.getClass().getName())) {
        throw new IllegalArgumentException("Must be instance of com.codahale.metrics.MetricRegistry");
      }
      this.metricsRegistry = metricsRegistry;
      return this;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
      return transactionManager;
    }

    @Override
    public SQLVariants getSQLVariants() {
      return sqlVariant;
    }

    @Override
    public ObjectMapper getObjectMapper() {
      return objectMapper;
    }

    @Override
    public String getProperty(String name) {
        String val = properties.get(name);
        if (val == null) {
            val = System.getProperty(name);
        }
        return val;
      }

    @Override
    public ThreadFactory getThreadFactory() {
      // TODO one with names isntead
      return Executors.defaultThreadFactory();
    }

    @Override
    public Object getMetricsRegistry() {
      return metricsRegistry;
    }
}
