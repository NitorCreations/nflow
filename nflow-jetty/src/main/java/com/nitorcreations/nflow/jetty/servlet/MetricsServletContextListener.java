package com.nitorcreations.nflow.jetty.servlet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static org.springframework.web.context.support.WebApplicationContextUtils.findWebApplicationContext;

public class MetricsServletContextListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ServletContext context = servletContextEvent.getServletContext();
    MetricRegistry metricRegistry = getSpringBean(MetricRegistry.class, context);
    HealthCheckRegistry healthCheckRegistry = getSpringBean(HealthCheckRegistry.class, context);
    context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY,healthCheckRegistry);
    context.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
  }

  private <T> T getSpringBean(Class<T> clazz, ServletContext context) {
    return findWebApplicationContext(context).getBean(clazz);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    // no operation
  }

}
