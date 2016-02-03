package com.nitorcreations.nflow.rest;

import com.nitorcreations.nflow.rest.config.DateTimeParamConverterProvider;
import com.nitorcreations.nflow.rest.v1.ArchiveResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;
import org.glassfish.jersey.simple.SimpleServer;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertNotEquals;

public class JerseyCompabilityTest {

  @Test
  public void restApiWorksInJersey() {
    URI baseUri = UriBuilder.fromUri("http://localhost/").port(0).build();
    ResourceConfig config = new JerseyResourceConfig();
    SimpleServer server = SimpleContainerFactory.create(baseUri, config);
    assertNotEquals(0, server.getPort());
  }

  public static class JerseyResourceConfig extends ResourceConfig {
    public JerseyResourceConfig() {
      packages(ArchiveResource.class.getPackage().getName());
      register(DateTimeParamConverterProvider.class);
    }
  }
}
