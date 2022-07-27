package io.nflow.engine.config;

import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.transaction.PlatformTransactionManager;

import io.nflow.engine.config.db.DatabaseConfiguration;

public interface NFlowConfiguration {
    
    DataSource getDataSource();

    String getProperty(String name);

    ObjectMapper getObjectMapper();

    PlatformTransactionManager getTransactionManager();

    Object getMetricsRegistry();

    DatabaseConfiguration getDatabaseConfiguration();

    ThreadFactory getThreadFactory();

    default <T> T getRequiredProperty(String name, Class<T> type) {
        T val = getProperty(name, type, null);
        if (val == null) {
            throw new IllegalArgumentException("Missing required property " + name);
        }
        return val;
      }

    @SuppressWarnings("unchecked")
    default <T> T getProperty(String name, Class<T> type, T defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        if (String.class.isAssignableFrom(type)) {
            return (T) val;
        }
        if (Long.class.isAssignableFrom(type)) {
            return (T) Long.valueOf(val);
        }
        if (Integer.class.isAssignableFrom(type)) {
            return (T) Integer.valueOf(val);
        }
        throw new IllegalArgumentException("Unsupported property type " + type);
    }
}
