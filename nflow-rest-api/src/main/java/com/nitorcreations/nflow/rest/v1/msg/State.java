package com.nitorcreations.nflow.rest.v1.msg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.LinkedHashSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(description = "Workflow definition states and transition to next states")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class State {

  public State() {
    // default constructor for Jackson deserializer
  }

  public State(String id, String type, String description) {
    this.id = id;
    this.type = type;
    this.description = description;
  }

  @ApiModelProperty(value = "State identifier", required = true)
  public String id;

  @ApiModelProperty(value = "State type", required = true)
  public String type;

  @ApiModelProperty(value = "State description", required = true)
  public String description;

  @ApiModelProperty("Alternative transitions from this state")
  public Set<String> transitions = new LinkedHashSet<>();

  @ApiModelProperty("Failure state for the this state")
  public String onFailure;
}
