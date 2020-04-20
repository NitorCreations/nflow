package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.Response.status;

import java.util.function.Supplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.nflow.rest.v1.ResourceBase;

public class JaxRsResource extends ResourceBase {

  protected Response handleExceptions(Supplier<ResponseBuilder> responseBuilder) {
    return handleExceptions(() -> responseBuilder.get().build(), this::toErrorResponse);
  }

  private Response toErrorResponse(int statusCode, Object body) {
    return status(statusCode).entity(body).build();
  }
}
