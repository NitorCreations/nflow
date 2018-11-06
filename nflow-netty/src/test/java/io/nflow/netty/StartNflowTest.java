package io.nflow.netty;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class StartNflowTest {

  @Test
  public void startNflowNetty() throws Exception {
    StartNflow startNflow = new StartNflow()
        .registerSpringContext(this.getClass())
        .registerSpringClasspathPropertySource("external.properties");
    Map<String, Object> properties = new HashMap<>();
    properties.put("nflow.db.create_on_startup", false);
    properties.put("nflow.autostart", false);
    properties.put("nflow.autoinit", false);
    ApplicationContext ctx = startNflow.startNetty(7500, "local", "", properties);

    assertEquals("7500", ctx.getEnvironment().getProperty("port"));
    assertEquals("local", ctx.getEnvironment().getProperty("env"));
    assertEquals("externallyDefinedExecutorGroup", ctx.getEnvironment().getProperty("nflow.executor.group"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.db.create_on_startup"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autostart"));
    assertEquals("false", ctx.getEnvironment().getProperty("nflow.autoinit"));
  }

}
