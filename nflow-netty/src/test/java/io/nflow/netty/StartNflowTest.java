package io.nflow.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

public class StartNflowTest {

  public class TestApplicationListener implements ApplicationListener<ApplicationContextEvent> {
    public ApplicationContextEvent applicationContextEvent;
    @Override
    public void onApplicationEvent(ApplicationContextEvent applicationContextEvent) {
      this.applicationContextEvent = applicationContextEvent;
    }
  }

  @Test
  public void startNflowNetty() throws Exception {
    TestApplicationListener testListener = new TestApplicationListener();
    StartNflow startNflow = new StartNflow().registerSpringContext(this.getClass())
        .registerSpringClasspathPropertySource("external.properties")
        .registerSpringApplicationListener(testListener);
    Map<String, Object> properties = new HashMap<>();
    properties.put("nflow.db.create_on_startup", false);
    properties.put("nflow.autostart", false);
    properties.put("nflow.autoinit", false);
    ApplicationContext ctx = startNflow.startNetty(7500, "local", "", properties);

    assertNotNull(testListener.applicationContextEvent);
    assertEquals("7500", ctx.getEnvironment().getProperty("port"));
    assertEquals("local", ctx.getEnvironment().getProperty("env"));
    assertEquals("externallyDefinedExecutorGroup", ctx.getEnvironment().getProperty("nflow.executor.group"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.db.create_on_startup"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autostart"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autoinit"));
  }

}
