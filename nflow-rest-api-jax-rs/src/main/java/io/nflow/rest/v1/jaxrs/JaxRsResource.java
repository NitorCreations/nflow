package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.function.Supplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.msg.ErrorResponse;

public class JaxRsResource extends ResourceBase {

  protected Response handleExceptions(Supplier<Response> response) {
    try {
      return response.get();
    } catch (IllegalArgumentException e) {
      return toErrorResponse(BAD_REQUEST, e.getMessage());
    } catch (NflowNotFoundException e) {
      return toErrorResponse(NOT_FOUND, e.getMessage());
    } catch (Throwable t) {
      return toErrorResponse(INTERNAL_SERVER_ERROR, t.getMessage());
    }
  }

  private Response toErrorResponse(Status status, String error) {
    return status(status).entity(new ErrorResponse(error)).build();
  }
}
