package io.nflow.jetty;

import static io.nflow.engine.config.Profiles.JMX;
import static io.nflow.rest.config.jaxrs.PathConstants.NFLOW_REST_JAXRS_PATH_PREFIX;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;
import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.dropwizard.metrics.servlets.AdminServlet;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.web.context.ContextLoaderListener;

import com.nitorcreations.core.utils.KillProcess;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.jetty.config.NflowJettyConfiguration;
import io.nflow.jetty.servlet.MetricsServletContextListener;
import io.nflow.jetty.spring.NflowAnnotationConfigWebApplicationContext;
import io.nflow.server.spring.NflowStandardEnvironment;

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
    return startJetty(port, env, profiles, new LinkedHashMap<>());
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
    String host = env.getRequiredProperty("host");
    int port = env.getRequiredProperty("port", Integer.class);
    KillProcess.gracefullyTerminateOrKillProcessUsingPort(port, env.getRequiredProperty("terminate.timeout", Integer.class), true);
    Server server = setupServer();
    setupJmx(server, env);
    setupServerConnector(server, host, port);
    ServletContextHandler context = setupServletContextHandler(env.getRequiredProperty("extra.resource.directories", String[].class));
    setupHandlers(server, context, env);
    setupSpring(context, env);
    setupCxf(context);
    setupMetricsAdminServlet(context);
    server.start();
    long end = currentTimeMillis();
    JettyServerContainer startedServer = new JettyServerContainer(server);
    port = startedServer.getPort();
    logger.info("Successfully started Jetty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0, Arrays.toString(env.getActiveProfiles()));
    logger.info("API available at http://{}:{}{}", host, port, NFLOW_REST_JAXRS_PATH_PREFIX);
    logger.info("Swagger available at http://{}:{}/nflow/ui/doc/", host, port);
    logger.info("Explorer available at http://{}:{}/nflow/ui/explorer/", host, port);
    logger.info("Metrics and health checks available at http://{}:{}/nflow/metrics/", host, port);
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

  protected void setupCxf(ServletContextHandler context) {
    ServletHolder servlet = context.addServlet(CXFServlet.class, NFLOW_REST_JAXRS_PATH_PREFIX + "/*");
    servlet.setDisplayName("nflow-cxf-services");
    servlet.setInitOrder(1);
  }

  protected void setupMetricsAdminServlet(ServletContextHandler context) {
    ServletHolder servlet = context.addServlet(AdminServlet.class, "/nflow/metrics/*");
    context.addEventListener(new MetricsServletContextListener());
    servlet.setDisplayName("nflow-metrics-admin-servlet");
    servlet.setInitOrder(2);
  }

  private Server setupServer() {
    Server server = new Server(new QueuedThreadPool(100));
    server.setStopAtShutdown(true);
    return server;
  }

  private void setupJmx(Container server, Environment env) {
    if (asList(env.getActiveProfiles()).contains(JMX)) {
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

  @SuppressWarnings("resource")
  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "Message is ok")
  private ServletContextHandler setupServletContextHandler(String... extraStaticResources) throws IOException {
    ServletContextHandler context = new ServletContextHandler(NO_SESSIONS | NO_SECURITY);

    // workaround for a jetty bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=364936
    Resource.setDefaultUseCaches(false);

    List<String> extraResources = new ArrayList<>();
    for (String path : extraStaticResources) {
      File f = new File(path);
      if (f.isDirectory()) {
        extraResources.add(f.getCanonicalFile().toURI().toURL().toString());
      }
    }
    // add all 'static' resource roots locations from classpath
    for (URL url : list(this.getClass().getClassLoader().getResources("static"))) {
      extraResources.add(url.toString());
    }
    if (!extraResources.isEmpty()) {
      context.setBaseResource(new ResourceCollection(extraResources.toArray(new String[extraResources.size()])));
      logger.info("Extra static resources served from {}", extraResources);
    }
    context.setWelcomeFiles(new String[] { "index.html", "service.json" });

    // add one 'nflow-ui-static' resource root
    String nflowUiResourceBase;
    List<URL> nflowUiResources = list(this.getClass().getClassLoader().getResources("nflow-ui-assets"));
    if (nflowUiResources.isEmpty()) {
      throw new RuntimeException("Could not find 'nflow-ui-assets' resource from classpath");
    } else if (nflowUiResources.size() > 1) {
      throw new RuntimeException(
          "Found more than one (" + nflowUiResources.size() + ") 'nflow-ui-assets' resources from classpath");
    } else {
      nflowUiResourceBase = nflowUiResources.get(0).toString();
    }
    logger.info("nFlow UI static resources served from {}", nflowUiResourceBase);

    ServletHolder holder = new ServletHolder(new DefaultServlet());
    holder.setInitParameter("resourceBase", nflowUiResourceBase);
    holder.setInitParameter("pathInfoOnly", "true");
    holder.setInitParameter("dirAllowed", "false");
    holder.setInitParameter("gzip", "true");
    holder.setInitParameter("acceptRanges", "false");
    holder.setDisplayName("nflow-static");
    holder.setInitOrder(1);

    context.addServlet(holder, "/nflow/ui/*");

    MimeTypes mimeTypes = context.getMimeTypes();
    mimeTypes.addMimeMapping("ttf", "application/font-sfnt");
    mimeTypes.addMimeMapping("otf", "application/font-sfnt");
    mimeTypes.addMimeMapping("woff", "application/font-woff");
    mimeTypes.addMimeMapping("eot", "application/vnd.ms-fontobject");
    mimeTypes.addMimeMapping("svg", "image/svg+xml");
    mimeTypes.addMimeMapping("html", "text/html; charset=utf-8");
    mimeTypes.addMimeMapping("css", "text/css; charset=utf-8");
    mimeTypes.addMimeMapping("js", "application/javascript; charset=utf-8");

    return context;
  }

  private void setupHandlers(final HandlerWrapper server, final Handler context, PropertyResolver env) {
    HandlerCollection handlers = new HandlerCollection();
    server.setHandler(handlers);
    handlers.addHandler(context);
    handlers.addHandler(createAccessLogHandler(env));
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private RequestLogHandler createAccessLogHandler(PropertyResolver env) {
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    String directory = env.getProperty("nflow.jetty.accesslog.directory", "log");
    new File(directory).mkdir();

    RequestLogWriter logWriter = new RequestLogWriter(Paths.get(directory, "yyyy_mm_dd.request.log").toString());
    String timeZoneId = TimeZone.getDefault().getID();
    logWriter.setTimeZone(timeZoneId);
    logWriter.setRetainDays(90);
    logWriter.setAppend(true);

    // Copy-paste and modify CustomRequestLog.EXTENDED_NCSA_FORMAT to use custom timestamp and log latency
    String logFormat = "%{client}a - %u %{yyyy-MM-dd:HH:mm:ss Z|" + timeZoneId + "}t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\" %{ms}T";
    requestLogHandler.setRequestLog(new CustomRequestLog(logWriter, logFormat));
    return requestLogHandler;
  }
}
