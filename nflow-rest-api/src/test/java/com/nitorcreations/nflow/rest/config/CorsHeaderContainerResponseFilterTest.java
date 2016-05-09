package com.nitorcreations.nflow.rest.config;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

@RunWith(MockitoJUnitRunner.class)
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

  @Before
  public void setup() {
    when(env.getRequiredProperty("nflow.rest.allow.origin")).thenReturn(HOST);
    when(env.getRequiredProperty("nflow.rest.allow.headers")).thenReturn(HEADERS);
    filter = new CorsHeaderContainerResponseFilter(env);
    when(responseContext.getHeaders()).thenReturn(headerMap);
  }

  @Test
  public void addsHeaders() {

    filter.filter(requestContext, responseContext);

    assertEquals(asList(HOST), headerMap.get("Access-Control-Allow-Origin"));
    assertEquals(asList(HEADERS), headerMap.get("Access-Control-Allow-Headers"));
    assertEquals(asList("OPTIONS, GET, POST, PUT, DELETE"), headerMap.get("Access-Control-Allow-Methods"));
    assertEquals(asList("true"), headerMap.get("Access-Control-Allow-Credentials"));
  }

}
