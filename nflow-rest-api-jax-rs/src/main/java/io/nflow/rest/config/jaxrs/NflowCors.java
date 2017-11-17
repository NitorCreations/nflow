package io.nflow.rest.config.jaxrs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.ws.rs.NameBinding;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface NflowCors {
  // annotation for marking JAX-RS resources that should be processed by CorsHeaderContainerResponseFilter
}
