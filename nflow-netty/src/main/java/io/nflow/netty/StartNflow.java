package io.nflow.netty;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import io.nflow.netty.config.NflowNettyConfiguration;
import io.nflow.server.spring.NflowStandardEnvironment;
import reactor.ipc.netty.http.server.HttpServer;

public class StartNflow {

  private static final Logger logger = LoggerFactory.getLogger(StartNflow.class);

  public static final String DEFAULT_SERVER_CONFIGURATION = "application.properties";
  public static final String DEFAULT_SERVER_HOST = "localhost";
  public static final Integer DEFAULT_SERVER_PORT = 7500;

  private final Optional<Class<?>> springMainClass;

  public static void main(String[] args) throws Exception {
    new StartNflow().startNetty(args, Optional.empty());
  }

  public StartNflow() {
    springMainClass = Optional.empty();
  }

  public StartNflow(Class<?> springMainClass) {
    this.springMainClass = Optional.of(springMainClass);
  }

  public ApplicationContext startNetty() throws IOException {
    return startNetty(true, true, true, Optional.empty());
  }

  public ApplicationContext startNetty(boolean createDatabase, boolean autoInitNflow, boolean autoStartNflow)
      throws IOException {
    return startNetty(new String[] {}, createDatabase, autoInitNflow, autoStartNflow,
        Optional.of(DEFAULT_SERVER_CONFIGURATION));
  }

  public ApplicationContext startNetty(boolean createDatabase, boolean autoInitNflow, boolean autoStartNflow,
      Optional<String> mainConfigurationClasspath) throws IOException {
    return startNetty(new String[] {}, createDatabase, autoInitNflow, autoStartNflow, mainConfigurationClasspath);
  }

  public ApplicationContext startNetty(String[] args, boolean createDatabase, boolean autoInitNflow,
      boolean autoStartNflow) throws IOException {
    return startNetty(args, createDatabase, autoInitNflow, autoStartNflow, Optional.of(DEFAULT_SERVER_CONFIGURATION));
  }

  public ApplicationContext startNetty(String[] args, boolean createDatabase, boolean autoInitNflow,
      boolean autoStartNflow, Optional<String> mainConfigurationClasspath) throws IOException {
    String[] customArgs = Arrays.copyOf(args, args.length + 3);
    customArgs[customArgs.length - 3] = "--nflow.db.create_on_startup=" + createDatabase;
    customArgs[customArgs.length - 2] = "--nflow.autostart=" + autoStartNflow;
    customArgs[customArgs.length - 1] = "--nflow.autoinit=" + autoInitNflow;
    return startNetty(customArgs, mainConfigurationClasspath);
  }

  public ApplicationContext startNetty(String[] args, Optional<String> mainConfigurationClasspath) throws IOException {
    long start = currentTimeMillis();
    Map<String, Object> argsMap = new HashMap<>();

    // Add optional arguments to map, using Spring's helper class
    SimpleCommandLinePropertySource arguments = new SimpleCommandLinePropertySource(args);
    Arrays.asList(arguments.getPropertyNames()).forEach(optionName -> {
      argsMap.put(optionName, arguments.getProperty(optionName));
    });

    // Add also properties that are not optional, i.e. don't start with "--"
    Stream.of(args).filter(value -> !value.startsWith("--")).forEach(value -> argsMap.put(value, null));

    // Create context
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    NflowStandardEnvironment env = new NflowStandardEnvironment(argsMap);
    Optional<ResourcePropertySource> mainConfiguration = initMainConfiguration(mainConfigurationClasspath, env);
    context.setEnvironment(env);
    if (springMainClass.isPresent()) {
      context.register(DelegatingWebFluxConfiguration.class, NflowNettyConfiguration.class, springMainClass.get());
    } else {
      context.register(DelegatingWebFluxConfiguration.class, NflowNettyConfiguration.class);
    }
    context.refresh();

    // Start netty
    HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).build();
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
    int port = getProperty(mainConfiguration, "port").map(Integer::valueOf).orElse(DEFAULT_SERVER_PORT);
    String host = getProperty(mainConfiguration, "host").orElse(DEFAULT_SERVER_HOST);
    HttpServer.create(host, port).newHandler(adapter).block();
    long end = currentTimeMillis();

    // Log info
    logger.info("Successfully started Netty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0,
        Arrays.toString(env.getActiveProfiles()));
    logger.info("API available at http://{}:{}/{}", host, port, context.getEnvironment().getProperty("nflow.rest.path.prefix"));
    logger.info("UI available at http://{}:{}/nflow/ui", host, port);

    return context;
  }

  private Optional<String> getProperty(Optional<ResourcePropertySource> configuration, String propertyName) {
    return Optional.ofNullable(configuration.map(config -> (String) config.getProperty(propertyName)).orElse(null));
  }

  private Optional<ResourcePropertySource> initMainConfiguration(Optional<String> mainConfigurationClasspath,
      NflowStandardEnvironment env) throws IOException {
    if (mainConfigurationClasspath.isPresent()){
      ResourcePropertySource mainConfiguration = new ResourcePropertySource(mainConfigurationClasspath.get());
      env.getPropertySources().addLast(mainConfiguration);
      return Optional.of(mainConfiguration);
    }
    return Optional.empty();
  }
}
