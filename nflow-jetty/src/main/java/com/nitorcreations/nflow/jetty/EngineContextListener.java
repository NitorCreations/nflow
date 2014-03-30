package com.nitorcreations.nflow.jetty;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.nitorcreations.nflow.engine.WorkflowDispatcher;

public class EngineContextListener implements ServletContextListener {

  private static final Logger LOG = getLogger(EngineContextListener.class);

  private ScheduledExecutorService executor;
  private WorkflowDispatcher dispatcher;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
    executor = Executors.newScheduledThreadPool(1, new CustomizableThreadFactory("nflow-"));
    dispatcher = springContext.getBean(WorkflowDispatcher.class);
    if (Boolean.getBoolean("disable.engine")) {
      LOG.info("nFlow engine not started (system property disable.engine=false)");
    } else {
      executor.execute(dispatcher);
      LOG.info("nFlow engine scheduled.");
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    dispatcher.shutdown();
    executor.shutdown();
  }

}
