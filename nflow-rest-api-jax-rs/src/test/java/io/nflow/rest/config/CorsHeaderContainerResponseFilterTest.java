package io.nflow.rest.config;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import io.nflow.rest.config.jaxrs.CorsHeaderContainerResponseFilter;

@ExtendWith(MockitoExtension.class)
public class CorsHeaderContainerResponseFilterTest {

  @Mock
  ContainerRequestContext requestContext;
  @Mock
  ContainerResponseContext responseContext;
  @Mock
  Environment env;
  CorsHeaderContainerResponseFilter filter;
  MultivaluedMap<String, Object> headerMap = new MultivaluedHashMap<>();
  private static final String HOST = "example.com";
  private static final String HEADERS = "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept";

  @Test
  public void addsHeaders() {
    when(env.getRequiredProperty("nflow.rest.allow.origin")).thenReturn(HOST);
    when(env.getRequiredProperty("nflow.rest.allow.headers")).thenReturn(HEADERS);
    when(env.getRequiredProperty("nflow.rest.cors.enabled", Boolean.class)).thenReturn(TRUE);
    filter = new CorsHeaderContainerResponseFilter(env);
    when(responseContext.getHeaders()).thenReturn(headerMap);
    filter.filter(requestContext, responseContext);

    assertEquals(asList(HOST), headerMap.get("Access-Control-Allow-Origin"));
    assertEquals(asList(HEADERS), headerMap.get("Access-Control-Allow-Headers"));
    assertEquals(asList("OPTIONS, GET, POST, PUT, DELETE"), headerMap.get("Access-Control-Allow-Methods"));
    assertEquals(asList("true"), headerMap.get("Access-Control-Allow-Credentials"));
    verifyNoInteractions(requestContext);
  }

  @Test
  public void doesNotAddHeadersWhenDisabled() {
    when(env.getRequiredProperty("nflow.rest.cors.enabled", Boolean.class)).thenReturn(FALSE);
    filter = new CorsHeaderContainerResponseFilter(env);

    filter.filter(requestContext, responseContext);

    verifyNoInteractions(requestContext, responseContext);
  }

}
