package io.nflow.rest;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;
import org.glassfish.jersey.simple.SimpleServer;
import org.junit.Test;

import io.nflow.rest.config.jaxrs.DateTimeParamConverterProvider;
import io.nflow.rest.v1.jaxrs.ArchiveResource;

public class JerseyCompabilityTest {

  @Test
  public void restApiWorksInJersey() throws IOException {
    URI baseUri = UriBuilder.fromUri("http://localhost/").port(0).build();
    ResourceConfig config = new JerseyResourceConfig();
    try (SimpleServer server = SimpleContainerFactory.create(baseUri, config)) {
      assertNotEquals(0, server.getPort());
    }
  }

  public static class JerseyResourceConfig extends ResourceConfig {
    public JerseyResourceConfig() {
      packages(ArchiveResource.class.getPackage().getName());
      register(DateTimeParamConverterProvider.class);
    }
  }
}
