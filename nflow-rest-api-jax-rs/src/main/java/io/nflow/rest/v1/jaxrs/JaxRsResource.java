package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.config.jaxrs.PathConstants.NFLOW_REST_JAXRS_PATH_PREFIX;
import static javax.ws.rs.core.Response.status;

import java.util.function.Supplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;
import io.swagger.v3.oas.annotations.servers.Server;

@Server(url = NFLOW_REST_JAXRS_PATH_PREFIX)
public class JaxRsResource extends ResourceBase {

  protected Response handleExceptions(Supplier<ResponseBuilder> responseBuilder) {
    return handleExceptions(() -> responseBuilder.get().build(), this::toErrorResponse);
  }

  private Response toErrorResponse(int statusCode, ErrorResponse body) {
    return status(statusCode).entity(body).build();
  }
}
