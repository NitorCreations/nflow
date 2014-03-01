package com.nitorcreations.nflow.rest.v0.msg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Basic information of workflow definition")
public class ListWorkflowDefinitionResponse {

  @ApiModelProperty(value = "Type of the workflow definition", required=true)
  public String type;

  @ApiModelProperty(value = "Version of the workflow definition", required=true)
  public int version;

  @ApiModelProperty(value = "Description workflow definition", required=false)
  public String description;

  @ApiModelProperty(value = "Workflow definition states and transitions", required=true)
  public States states = new States();

  public static class States {
    @ApiModelProperty(value = "Alternate initial (start) states of the workflow", required=true)
    public List<String> initial;
    @ApiModelProperty(value = "Generic error state (e.g. when failure state not defined)", required=true)
    public String onError;
    @ApiModelProperty(value = "Allowed state transitions", required=true)
    public List<Transition> transitions = new ArrayList<>();
  }

  public static class Transition {
    @ApiModelProperty(value = "Source state of the transition", required=true)
    public String source;
    @ApiModelProperty(value = "Allowed target states from the source state", required=true)
    public Set<String> targets = new HashSet<>();
    @ApiModelProperty(value = "Generic failure state for the source state", required=false)
    public String onFailure;
  }

}
