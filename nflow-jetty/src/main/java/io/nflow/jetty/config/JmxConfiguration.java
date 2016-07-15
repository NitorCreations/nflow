package io.nflow.jetty.config;

import static io.nflow.engine.internal.config.Profiles.JMX;
import static java.lang.Boolean.TRUE;

import org.apache.cxf.Bus;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(JMX)
@Configuration
public class JmxConfiguration {

  @Bean
  public CounterRepository counterRepository(Bus cxf) {
    CounterRepository repository = new CounterRepository();
    repository.setBus(cxf);
    return repository;
  }

  @Bean
  public InstrumentationManager instrumentationManager(Bus cxf) {
    InstrumentationManagerImpl impl = new InstrumentationManagerImpl();
    impl.setEnabled(true);
    impl.setBus(cxf);
    impl.setUsePlatformMBeanServer(TRUE);
    return impl;
  }

}
