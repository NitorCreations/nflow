package com.nitorcreations.nflow.jetty.config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import com.wordnik.swagger.jaxrs.config.BeanConfig;

@RunWith(MockitoJUnitRunner.class)
public class NflowJettyConfigurationTest {

  @Mock
  private Environment env;

  @Test
  public void swaggerBasepathBuildingWorks() {
    when(env.getProperty(eq("swagger.basepath.protocol"), anyString())).thenReturn("https");
    when(env.getProperty(eq("swagger.basepath.server"), anyString())).thenReturn("foobar");
    when(env.getProperty(eq("swagger.basepath.port"), eq(Integer.class), anyInt())).thenReturn(123);
    when(env.getProperty(eq("swagger.basepath.context"), anyString())).thenReturn("/nflow");
    NflowJettyConfiguration config = new NflowJettyConfiguration();
    config.env = env;
    BeanConfig swaggerConfig = config.swaggerConfig();
    assertThat(swaggerConfig.getBasePath(), is("https://foobar:123/nflow"));
  }

  @Test
  public void swaggerBasepathPropertyWorks() {
    when(env.getProperty("swagger.basepath")).thenReturn("https://bar:123/foo");
    NflowJettyConfiguration config = new NflowJettyConfiguration();
    config.env = env;
    BeanConfig swaggerConfig = config.swaggerConfig();
    assertThat(swaggerConfig.getBasePath(), is("https://bar:123/foo"));
  }

}
