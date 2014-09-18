package com.nitorcreations.nflow.jetty.config;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("jmx")
@Configuration
public class JmxConfiguration {

  @Bean
  public CounterRepository counterRepository(SpringBus cxf) {
    CounterRepository repository = new CounterRepository();
    repository.setBus(cxf);
    return repository;
  }

  @Bean
  public InstrumentationManager instrumentationManager(SpringBus cxf) {
    InstrumentationManagerImpl impl = new InstrumentationManagerImpl();
    impl.setEnabled(true);
    impl.setBus(cxf);
    impl.setUsePlatformMBeanServer(true);
    return impl;
  }

}
