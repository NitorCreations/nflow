package io.nflow.netty;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class StartNflowTest {

  @Test
  public void startNflowNetty() throws Exception {
    StartNflow startNflow = new StartNflow();
    ApplicationContext ctx = startNflow.startNetty();
    assertNotNull(ctx);
  }

}
