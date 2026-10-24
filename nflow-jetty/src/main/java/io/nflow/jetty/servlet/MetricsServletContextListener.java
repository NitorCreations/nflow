package io.nflow.jetty.servlet;

import static org.springframework.web.context.support.WebApplicationContextUtils.findWebApplicationContext;

import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Optional;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;


public class MetricsServletContextListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ServletContext context = servletContextEvent.getServletContext();
    Optional<MetricRegistry> metricRegistry = getSpringBean(MetricRegistry.class, context);
    metricRegistry.ifPresent(reg -> context.setAttribute(MetricsServlet.METRICS_REGISTRY, reg));
    Optional<HealthCheckRegistry> healthCheckRegistry = getSpringBean(HealthCheckRegistry.class, context);
    healthCheckRegistry.ifPresent(reg -> context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, reg));
  }

  private <T> Optional<T> getSpringBean(Class<T> clazz, ServletContext context) {
    return Optional.ofNullable(findWebApplicationContext(context)).map(ctx -> ctx.getBean(clazz));
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    // no operation
  }

}
