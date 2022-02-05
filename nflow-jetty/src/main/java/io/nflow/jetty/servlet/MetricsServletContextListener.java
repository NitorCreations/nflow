package io.nflow.jetty.servlet;

import static org.springframework.web.context.support.WebApplicationContextUtils.findWebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MetricsServletContextListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ServletContext context = servletContextEvent.getServletContext();
    MetricRegistry metricRegistry = getSpringBean(MetricRegistry.class, context);
    HealthCheckRegistry healthCheckRegistry = getSpringBean(HealthCheckRegistry.class, context);
    context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY,healthCheckRegistry);
    context.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "npe is unlikely")
  private <T> T getSpringBean(Class<T> clazz, ServletContext context) {
    return findWebApplicationContext(context).getBean(clazz);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    // no operation
  }

}
