package io.nflow.jetty.servlet;

import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;

import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;


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
    return getRequiredWebApplicationContext(context).getBean(clazz);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    // no operation
  }

}
