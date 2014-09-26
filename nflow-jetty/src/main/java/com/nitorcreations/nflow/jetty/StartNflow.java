package com.nitorcreations.nflow.jetty;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;
import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ContextLoaderListener;

import com.nitorcreations.core.utils.KillProcess;
import com.nitorcreations.nflow.jetty.config.NflowJettyConfiguration;
import com.nitorcreations.nflow.jetty.spring.NflowAnnotationConfigWebApplicationContext;
import com.nitorcreations.nflow.jetty.spring.NflowStandardEnvironment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StartNflow
{
  private static final Logger logger = LoggerFactory.getLogger(StartNflow.class);

  private final Set<Class<?>> annotatedContextClasses = new LinkedHashSet<>();

  public static void main(final String... args) throws Exception {
    new StartNflow().startJetty(Collections.<String, Object>emptyMap());
  }

  public StartNflow registerSpringContext(Class<?> ... springContextClass) {
    annotatedContextClasses.addAll(asList(springContextClass));
    return this;
  }

  public JettyServerContainer startJetty(int port, String env, String profiles) throws Exception {
    return startJetty(port, env, profiles, new LinkedHashMap<String, Object>());
  }

  public JettyServerContainer startJetty(int port, String env, String profiles, Map<String, Object> properties) throws Exception {
    properties.put("port", port);
    properties.put("env", env);
    properties.put("profiles", profiles);
    return startJetty(properties);
  }

  public JettyServerContainer startJetty(Map<String, Object> properties) throws Exception {
    long start = currentTimeMillis();
    // also CXF uses JDK logging
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    ConfigurableEnvironment env = new NflowStandardEnvironment(properties);
    String host = env.getProperty("host", "localhost");
    int port = env.getProperty("port", Integer.class, 7500);
    KillProcess.killProcessUsingPort(port);
    Server server = setupServer();
    setupJmx(server, env);
    setupServerConnector(server, host, port);
    ServletContextHandler context = setupServletContextHandler();
    setupHandlers(server, context);
    setupSpring(context, env);
    setupCxf(context);
    server.start();
    long end = currentTimeMillis();
    JettyServerContainer startedServer = new JettyServerContainer(server);
    port = startedServer.getPort();
    logger.info("Successfully started Jetty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0, Arrays.toString(env.getActiveProfiles()));
    logger.info("API available at http://{}:{}/", host, port);
    logger.info("API doc available at http://{}:{}/ui", host, port);
    return startedServer;
  }

  @SuppressWarnings("resource")
  protected void setupSpring(final ServletContextHandler context, ConfigurableEnvironment env) {
    NflowAnnotationConfigWebApplicationContext webContext = new NflowAnnotationConfigWebApplicationContext(env);
    if(!annotatedContextClasses.isEmpty()) {
      webContext.register(annotatedContextClasses.toArray(new Class<?>[annotatedContextClasses.size()]));
    }
    context.addEventListener(new ContextLoaderListener(webContext));
    context.setInitParameter("contextConfigLocation", NflowJettyConfiguration.class.getName());
  }

  protected void setupCxf(final ServletContextHandler context) {
    ServletHolder servlet = context.addServlet(CXFServlet.class, "/*");
    servlet.setDisplayName("cxf-services");
    servlet.setInitOrder(1);
    servlet.setInitParameter("redirects-list", "/favicon.ico");
    servlet.setInitParameter("redirect-servlet-name", "default");
  }

  private Server setupServer() {
    Server server = new Server(new QueuedThreadPool(100));
    server.setStopAtShutdown(true);
    return server;
  }

  private void setupJmx(Server server, Environment env) {
    if (asList(env.getActiveProfiles()).contains("jmx")) {
      MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
      server.addEventListener(mbContainer);
      server.addBean(mbContainer);
    }
  }

  private void setupServerConnector(Server server, String host, int port) {
    @SuppressWarnings("resource")
    ServerConnector connector = new ServerConnector(server);
    connector.setHost(host);
    connector.setPort(port);
    connector.setIdleTimeout(TimeUnit.MINUTES.toMillis(2));
    connector.setReuseAddress(true);
    connector.setName(valueOf(port));
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

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
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
}
