package com.nitorcreations.nflow.rest.config;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.core.env.Environment;

/**
 * Filter to add headers to allow Cross-Origin Resource Sharing.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS</a>
 */
@Provider
public class CorsHeaderContainerResponseFilter implements ContainerResponseFilter {

  private final Environment env;

  @Inject
  public CorsHeaderContainerResponseFilter(final Environment env) {
    this.env = env;
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    String origin = env.getRequiredProperty("nflow.rest.allow.origin");
    responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
    responseContext.getHeaders().add("Access-Control-Allow-Headers",
        "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept");
    responseContext.getHeaders().add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
    // for cookies?
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
  }
}