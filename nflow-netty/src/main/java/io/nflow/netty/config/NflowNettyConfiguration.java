package io.nflow.netty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nflow.engine.config.NFlow;
import io.nflow.rest.config.RestConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.temporaryRedirect;

@Configuration
@ComponentScan("io.nflow.rest.v1.springweb")
@Import(RestConfiguration.class)
@EnableTransactionManagement
@EnableWebFlux
public class NflowNettyConfiguration implements WebFluxConfigurer {

  @Bean
  public PlatformTransactionManager transactionManager(@NFlow DataSource nflowDataSource) {
    return new DataSourceTransactionManager(nflowDataSource);
  }

  @Bean
  public DispatcherHandler webHandler(ApplicationContext context) {
    return new DispatcherHandler(context);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/nflow/ui/**").addResourceLocations("classpath:/nflow-ui-assets/");
  }

  @Bean
  public RouterFunction<ServerResponse> uiIndexRouter(@Value("classpath:/nflow-ui-assets/index.html") final Resource indexHtml) {
    return route(GET("/nflow/ui"), request -> ok().contentType(MediaType.TEXT_HTML).bodyValue(indexHtml));
  }

  @Bean
  public RouterFunction<ServerResponse> uiExplorerIndexRouter() {
    URI uri;
    try {
      uri = new URI("/nflow/ui/explorer/index.html");
    } catch (URISyntaxException e){
      throw new RuntimeException(e);
    }
    return route(GET("/nflow/ui/explorer"), request -> temporaryRedirect(uri).build());
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().findModulesViaServiceLoader(true).build();
    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
  }
}
