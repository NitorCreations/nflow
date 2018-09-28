package io.nflow.tests.demo.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AbstractResponse {

  public boolean success;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
