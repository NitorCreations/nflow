package com.nitorcreations.nflow.jetty;

import static java.lang.Integer.getInteger;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;

import com.nitorcreations.core.utils.KillProcess;
import com.nitorcreations.nflow.jetty.config.NflowJettyConfiguration;
import com.nitorcreations.nflow.jetty.spring.NflowAnnotationConfigWebApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class StartNflow
{
  private static final Logger LOG = LoggerFactory.getLogger(StartNflow.class);

  public static void main(final String... args) throws Exception {
    new StartNflow()
      .startTcpServerForH2()
      .startJetty(getInteger("port", 7500), getProperty("env", "dev"));
  }

  public Server startJetty(final int port, final String env) throws Exception {
    long start = currentTimeMillis();
    // also CXF uses JDK logging
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    setProperty("env", env);
    KillProcess.killProcessUsingPort(port);
    Server server = setupServer();
    setupServerConnector(server, port);
    ServletContextHandler context = setupServletContextHandler();
    setupHandlers(server, context);
    setupSpringAndCxf(context, env);
    setupNflowEngine(context);
    server.start();
    long end = currentTimeMillis();
    LOG.info("Successfully started Jetty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0, env);
    LOG.info("API available at http://localhost:" + port + "/");
    LOG.info("API doc available at http://localhost:" + port + "/ui");
    return server;
  }

  private void setupNflowEngine(ServletContextHandler context) {
    context.addEventListener(new EngineContextListener());
  }

  public StartNflow startTcpServerForH2() throws SQLException {
    org.h2.tools.Server h2Server = org.h2.tools.Server.createTcpServer(new String[] {"-tcp","-tcpAllowOthers","-tcpPort","8043"});
    h2Server.start();
    return this;
  }

  protected void setupSpringAndCxf(final ServletContextHandler context, String env) {
    ServletHolder servlet = context.addServlet(CXFServlet.class, "/*");
    servlet.setDisplayName("cxf-services");
    servlet.setInitOrder(1);
    servlet.setInitParameter("redirects-list", "/favicon.ico");
    servlet.setInitParameter("redirect-servlet-name", "default");
    context.addEventListener(new ContextLoaderListener());
    context.setInitParameter("contextClass", NflowAnnotationConfigWebApplicationContext.class.getName() );
    context.setInitParameter("contextConfigLocation", NflowJettyConfiguration.class.getName());
    String envs = env;
    if (enableJmx()) {
      envs += ",jmx";
    }
    setProperty("spring.profiles.active", envs);
  }

  private Server setupServer() {
    Server server = new Server(new QueuedThreadPool(100));
    server.setStopAtShutdown(true);
    if (enableJmx()) {
      MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
      server.addEventListener(mbContainer);
      server.addBean(mbContainer);
    }
    return server;
  }

  private void setupServerConnector(final Server server, final int port) {
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    connector.setIdleTimeout(TimeUnit.MINUTES.toMillis(2));
    connector.setReuseAddress(true);
    server.addConnector(connector);
  }

  private ServletContextHandler setupServletContextHandler() {
    ServletContextHandler context = new ServletContextHandler(NO_SESSIONS | NO_SECURITY);
    context.setResourceBase(getClass().getClassLoader().getResource("static").toExternalForm());
    context.setDisplayName("nflow-static");
    context.setStopTimeout(SECONDS.toMillis(10));
//    context.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")), "/*", EnumSet.allOf(DispatcherType.class));
    ServletHolder holder = new ServletHolder("default", DefaultServlet.class);
    context.addServlet(holder, "/ui/*");
    return context;
  }

  private void setupHandlers(final Server server, final ServletContextHandler context) {
    HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);
    handlers.addHandler(context);
    handlers.addHandler(createAccessLogHandler());
  }

  @SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private RequestLogHandler createAccessLogHandler() {
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    new File("log").mkdir();
    NCSARequestLog requestLog = new NCSARequestLog(Paths.get("log", "yyyy_mm_dd.request.log").toString());
    requestLog.setRetainDays(90);
    requestLog.setAppend(true);
    requestLog.setLogDateFormat("yyyy-MM-dd:HH:mm:ss Z");
    requestLog.setExtended(true);
    requestLog.setLogTimeZone(TimeZone.getDefault().getID());
    requestLog.setPreferProxiedForAddress(true);
    requestLog.setLogLatency(true);
    requestLogHandler.setRequestLog(requestLog);
    return requestLogHandler;
  }

  protected boolean enableJmx() {
    return Boolean.getBoolean("nflow.jmx");
  }

}
