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

  @Before
  public void setup() {
    filter = new CorsHeaderContainerResponseFilter(env);
    when(responseContext.getHeaders()).thenReturn(headerMap);
  }

  @Test
  public void addsHeaders() {
    String host="example.com";
    when(env.getRequiredProperty("nflow.rest.allow.origin")).thenReturn(host);

    filter.filter(requestContext, responseContext);

    assertEquals(asList(host), headerMap.get("Access-Control-Allow-Origin"));
    assertEquals(asList("X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept"),
        headerMap.get("Access-Control-Allow-Headers"));
    assertEquals(asList("OPTIONS, GET, POST, PUT, DELETE"), headerMap.get("Access-Control-Allow-Methods"));
    assertEquals(asList("true"), headerMap.get("Access-Control-Allow-Credentials"));
  }

}
