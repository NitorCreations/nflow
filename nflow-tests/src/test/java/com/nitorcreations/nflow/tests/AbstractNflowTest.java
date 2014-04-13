package com.nitorcreations.nflow.tests;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import com.nitorcreations.nflow.tests.config.PropertiesConfiguration;
import com.nitorcreations.nflow.tests.config.RestClientConfiguration;
import com.nitorcreations.nflow.tests.runner.NflowRunner;

@RunWith(NflowRunner.class)
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public abstract class AbstractNflowTest {

  @Inject
  @Named("workflow-instance")
  WebClient workflowInstanceResource;

}
