package io.nflow.netty;

import static java.util.Arrays.asList;
import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import io.nflow.netty.config.NflowNettyConfiguration;
import io.nflow.server.spring.NflowStandardEnvironment;
import reactor.netty.http.server.HttpServer;

public class StartNflow {

  private static final Logger logger = LoggerFactory.getLogger(StartNflow.class);

  private final Set<Class<?>> annotatedContextClasses = new LinkedHashSet<>();

  private final List<ApplicationListener<?>> applicationListeners = new LinkedList<>();

  private final List<ResourcePropertySource> propertiesSources = new LinkedList<>();

  public static void main(String[] args) throws Exception {
    Map<String, Object> argsMap = new HashMap<>();

    // Add optional arguments to map, using Spring's helper class
    SimpleCommandLinePropertySource arguments = new SimpleCommandLinePropertySource(args);
    Arrays.asList(arguments.getPropertyNames()).forEach(optionName -> {
      argsMap.put(optionName, arguments.getProperty(optionName));
    });

    // Add also properties that are not optional, i.e. don't start with "--"
    Stream.of(args).filter(value -> !value.startsWith("--")).forEach(value -> argsMap.put(value, null));

    new StartNflow().startNetty(argsMap);
  }

  public StartNflow registerSpringContext(Class<?>... springContextClasses) {
    annotatedContextClasses.addAll(asList(springContextClasses));
    return this;
  }

  public StartNflow registerSpringApplicationListener(ApplicationListener<?>... listeners) {
    this.applicationListeners.addAll(asList(listeners));
    return this;
  }

  public StartNflow registerSpringClasspathPropertySource(String... springPropertiesPaths) throws IOException {
    for (String path : springPropertiesPaths) {
      propertiesSources.add(new ResourcePropertySource(path));
    }
    return this;
  }

  public StartNflow registerSpringPropertySource(ResourcePropertySource... springPropertySources) {
    propertiesSources.addAll(asList(springPropertySources));
    return this;
  }

  public ApplicationContext startNetty(int port, String env, String profiles) throws Exception {
    return startNetty(port, env, profiles, new LinkedHashMap<>());
  }

  public ApplicationContext startNetty(int port, String env, String profiles, Map<String, Object> properties) throws Exception {
    properties.put("port", port);
    properties.put("env", env);
    properties.put("profiles", profiles);
    return startNetty(properties);
  }

  public ApplicationContext startNetty(Map<String, Object> properties) throws Exception {
    long start = currentTimeMillis();

    // Create context
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    NflowStandardEnvironment env = new NflowStandardEnvironment(properties);
    propertiesSources.stream().forEach(propertiesSource -> env.getPropertySources().addLast(propertiesSource));
    context.setEnvironment(env);
    annotatedContextClasses.add(DelegatingWebFluxConfiguration.class);
    annotatedContextClasses.add(NflowNettyConfiguration.class);
    context.register(annotatedContextClasses.stream().toArray(Class<?>[]::new));
    applicationListeners.forEach(applicationListener -> context.addApplicationListener(applicationListener));
    context.refresh();

    // Start netty
    HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context).build();
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
    int port = env.getRequiredProperty("port", Integer.class);
    String host = env.getRequiredProperty("host");
    HttpServer.create().host(host).port(port).handle(adapter).bind().block();
    long end = currentTimeMillis();

    // Log info
    logger.info("Successfully started Netty on port {} in {} seconds in environment {}", port, (end - start) / 1000.0,
        Arrays.toString(env.getActiveProfiles()));
    logger.info("API available at http://{}:{}/{}", host, port, context.getEnvironment().getProperty("nflow.rest.path.prefix"));
    logger.info("UI available at http://{}:{}/nflow/ui", host, port);

    return context;
  }

}
