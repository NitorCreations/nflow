package com.nitorcreations.nflow.rest.config;

import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BadRequestExceptionMapperTest {
  BadRequestExceptionMapper mapper = new BadRequestExceptionMapper();

  @Test
  public void badRequestExceptionResultInStatusBadRequest() {
    Response response = mapper.toResponse(new BadRequestException());
    assertThat(response.getStatus(), is(400));
  }
}
