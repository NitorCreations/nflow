package com.nitorcreations.nflow.jetty;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ext.Provider;

/**
 * Filter to add headers to allow Cross-Origin Resource Sharing.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
 */
@Provider
public class CorsHeaderFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse httpResponse = (HttpServletResponse)  response;
    httpResponse.addHeader("Access-Control-Allow-Origin", "*");
    httpResponse.addHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept");
    httpResponse.addHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
    // for cookies?
    httpResponse.addHeader("Access-Control-Allow-Credentials", "true");
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // no-op
  }

  @Override
  public void destroy() {
    // no-op
  }
}

