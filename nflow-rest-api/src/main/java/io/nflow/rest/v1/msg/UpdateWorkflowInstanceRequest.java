package io.nflow.rest.v1.msg;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.model.ModelObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request for update workflow instance")
@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="jackson reads dto fields")
public class UpdateWorkflowInstanceRequest extends ModelObject {

  @ApiModelProperty("New state of the workflow instance")
  public String state;

  @ApiModelProperty("New next activation time for next workflow instance processing")
  public DateTime nextActivationTime;

  @ApiModelProperty("Description of the action")
  public String actionDescription;
}
