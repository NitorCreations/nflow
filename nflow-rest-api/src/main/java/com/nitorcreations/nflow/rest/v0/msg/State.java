package com.nitorcreations.nflow.rest.v0.msg;

import java.util.LinkedHashSet;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Workflow definition states and transition to next states")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class State {

  public State(String id, String type, String name, String description) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.description = description;
  }

  @ApiModelProperty(value = "State identifier", required=true)
  public String id;

  @ApiModelProperty(value = "State type", required=true)
  public String type;

  @ApiModelProperty(value = "State name", required=true)
  public String name;

  @ApiModelProperty(value = "State description", required=true)
  public String description;

  @ApiModelProperty(value = "Alternative transitions from this state", required=false)
  public Set<String> transitions = new LinkedHashSet<>();

  @ApiModelProperty(value = "Failure state for the this state", required=false)
  public String onFailure;
}
