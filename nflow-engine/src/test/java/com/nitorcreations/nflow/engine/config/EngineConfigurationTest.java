package com.nitorcreations.nflow.engine.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RunWith(MockitoJUnitRunner.class)
public class EngineConfigurationTest {

  @Mock
  private Environment environment;

  private EngineConfiguration configuration;

  @Before
  public void setup() {
    configuration = new EngineConfiguration();
    configuration.env = environment;
    when(environment.getRequiredProperty("executor.thread.count", Integer.class)).thenReturn(100);
  }

  @Test
  public void dispatcherPoolExecutorInstantiation() {
    ThreadPoolTaskExecutor executor = configuration.dispatcherPoolExecutor();
    assertThat(executor.getCorePoolSize(), is(100));
    assertThat(executor.getMaxPoolSize(), is(100));
    assertThat(executor.getKeepAliveSeconds(), is(0));
  }

  @Test
  public void workflowDefinitionListingInstantiated() {
    assertThat(configuration.workflowDefinitionListing(), notNullValue());
  }

}
