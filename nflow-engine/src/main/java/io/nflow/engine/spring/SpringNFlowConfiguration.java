package io.nflow.engine.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.internal.storage.db.SQLVariants;

@Component
public class SpringNFlowConfiguration implements NFlowConfiguration {
    
    private final Environment env;
    private final DataSource dataSource;
    private final SQLVariants sqlVariants;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final ThreadFactory threadFactory;

    @Inject
    SpringNFlowConfiguration(Environment env,@NFlow DataSource dataSource,
    SQLVariants sqlVariants, @NFlow ObjectMapper objectMapper,
    PlatformTransactionManager transactionManager, @NFlow ThreadFactory threadFactory) {
      this.env = env;
      this.dataSource = dataSource;
      this.sqlVariants = sqlVariants;
      this.objectMapper = objectMapper;
      this.transactionManager = transactionManager;
      this.threadFactory = threadFactory;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public SQLVariants getSQLVariants() {
      return sqlVariants;
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
