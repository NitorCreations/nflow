package com.nitorcreations.nflow.rest.config;

import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NotFoundExceptionMapperTest {
  NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();

  @Test
  public void notFoundExceptionResultInStatusNotFound() {
    Response response = mapper.toResponse(new NotFoundException());
    assertThat(response.getStatus(), is(404));
  }
}
