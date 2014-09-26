package com.nitorcreations.nflow.jetty;

import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorsHeaderFilterTest {
  CorsHeaderFilter filter = new CorsHeaderFilter();
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  FilterChain chain;

  @Test
  public void doFilterAddsHeaders() throws IOException, ServletException {
    filter.doFilter(request, response, chain);
    verify(response).addHeader("Access-Control-Allow-Origin", "*");
    verify(response).addHeader("Access-Control-Allow-Headers",
        "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept");
    verify(response).addHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
    verify(chain).doFilter(request, response);
  }
}
