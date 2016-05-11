package com.nitorcreations.nflow.rest.config;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.core.env.Environment;

/**
 * Filter to add headers to allow Cross-Origin Resource Sharing. Applied only to JAX-RS resources that are annotated with
 * {@code NflowCors} annotation.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS</a>
 */
@Provider
@NflowCors
public class CorsHeaderContainerResponseFilter implements ContainerResponseFilter {

  private final boolean enabled;
  private final String origin;
  private final String headers;

  @Inject
  public CorsHeaderContainerResponseFilter(final Environment env) {
    enabled = env.getRequiredProperty("nflow.rest.cors.enabled", Boolean.class);
    origin = env.getRequiredProperty("nflow.rest.allow.origin");
    headers = env.getRequiredProperty("nflow.rest.allow.headers");
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    if (enabled) {
      responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
      responseContext.getHeaders().add("Access-Control-Allow-Headers", headers);
      responseContext.getHeaders().add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
      // for cookies?
      responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
    }
  }
}
