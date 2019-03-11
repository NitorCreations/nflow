package io.nflow.jetty.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
public class NflowJettyConfigurationTest {

  @Mock
  private Environment env;

  @Test
  public void createsValidationExceptionMapper() {
    assertThat(new NflowJettyConfiguration().validationExceptionMapper(), notNullValue());
  }
}
