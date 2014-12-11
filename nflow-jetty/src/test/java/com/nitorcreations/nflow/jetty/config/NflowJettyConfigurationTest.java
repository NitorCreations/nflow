package com.nitorcreations.nflow.jetty.config;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

@RunWith(MockitoJUnitRunner.class)
public class NflowJettyConfigurationTest {

  @Mock
  private Environment env;

  @Test
  public void createsValidationExceptionMapper() {
    assertThat(new NflowJettyConfiguration().validationExceptionMapper(), notNullValue());
  }
}
