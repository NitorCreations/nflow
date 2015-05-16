package com.nitorcreations.nflow.rest.config;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;

public class NotFoundExceptionMapperTest {
  NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();

  @Test
  public void notFoundExceptionResultInStatusNotFound() {
    Response response = mapper.toResponse(new NotFoundException());
    assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
  }
}
