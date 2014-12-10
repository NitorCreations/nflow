package com.nitorcreations.nflow.rest.v1.msg;

import java.util.LinkedHashSet;
import java.util.Set;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ApiModel(value = "Workflow definition states and transition to next states")
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads dto fields")
public class State {

  public State() {
    // default constructor for Jackson deserializer
  }

  /**
   * Create a state.
   *
   * @deprecated Use the version that does not take name parameter. This
   *             constructor will be removed in 2.0.
   * @param id
   *          The state identifier.
   * @param type
   *          The state type.
   * @param name
   *          The state name.
   * @param description
   *          The state description.
   */
  @Deprecated
  public State(String id, String type, String name, String description) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.description = description;
  }

  public State(String id, String type, String description) {
    this.id = id;
    this.type = type;
    this.name = id;
    this.description = description;
  }

  @ApiModelProperty(value = "State identifier", required = true)
  public String id;

  @ApiModelProperty(value = "State type", required = true)
  public String type;

  /**
   * @deprecated Use id instead. Will be removed in 2.0.
   */
  @Deprecated
  @ApiModelProperty(value = "State name", required = true)
  public String name;

  @ApiModelProperty(value = "State description", required = true)
  public String description;

  @ApiModelProperty(value = "Alternative transitions from this state", required = false)
  public Set<String> transitions = new LinkedHashSet<>();

  @ApiModelProperty(value = "Failure state for the this state", required = false)
  public String onFailure;
}
