package com.nitorcreations.nflow.rest.v0.msg;

import java.util.HashSet;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Basic information of workflow definition")
public class ListWorkflowDefinitionResponse {

  @ApiModelProperty(value = "Type of the workflow definition", required=true)
  public String type;

  @ApiModelProperty(value = "Name of the workflow definition", required=true)
  public String name;

  @ApiModelProperty(value = "Description workflow definition", required=false)
  public String description;

  @ApiModelProperty(value = "Version of the workflow definition", required=true)
  public int version;

  @ApiModelProperty(value = "Generic error state", required=true)
  public String onError;

  @ApiModelProperty(value = "Workflow definition states and transitions", required=true)
  public State[] states;

  public static class State {

    public State(String id, String type) {
      this.id = id;
      this.type = type;
    }

    @ApiModelProperty(value = "State identifier", required=true)
    public String id;

    @ApiModelProperty(value = "State type", required=true)
    public String type;

    @ApiModelProperty(value = "Alternative transitions from this state", required=true)
    public Set<String> transitions = new HashSet<>();

    @ApiModelProperty(value = "Failure state for the this state", required=false)
    public String onFailure;
  }

}
