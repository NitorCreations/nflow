package io.nflow.rest.v1;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.lang.Boolean.parseBoolean;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.joda.time.DateTime.now;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.dao.EmptyResultDataAccessException;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.service.NflowNotFoundException;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import io.nflow.rest.v1.msg.ErrorResponse;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;

/**
 * Common base class for REST API logic
 */
public abstract class ResourceBase {

  protected static final String currentStateVariables = "currentStateVariables";
  protected static final String actions = "actions";
  protected static final String actionStateVariables = "actionStateVariables";
  protected static final String childWorkflows = "childWorkflows";
  protected static final String INCLUDE_PARAM_VALUES = currentStateVariables + "," + actions + "," + actionStateVariables + ","
      + childWorkflows;
  protected static final String INCLUDE_PARAM_DESC = "Data to include in response. " + currentStateVariables
      + " = current stateVariables for worfklow, " + actions + " = state transitions, " + actionStateVariables
      + " = state variable changes for actions, " + childWorkflows + " = map of created child workflow instance IDs by action ID";
  private static final Map<String, WorkflowInstanceInclude> INCLUDE_STRING_TO_ENUM = unmodifiableMap(Stream
      .of(new SimpleEntry<>(currentStateVariables, WorkflowInstanceInclude.CURRENT_STATE_VARIABLES),
          new SimpleEntry<>(actions, WorkflowInstanceInclude.ACTIONS),
          new SimpleEntry<>(actionStateVariables, WorkflowInstanceInclude.ACTION_STATE_VARIABLES),
          new SimpleEntry<>(childWorkflows, WorkflowInstanceInclude.CHILD_WORKFLOW_IDS))
      .collect(toMap(Entry::getKey, Entry::getValue)));
  protected static final String QUERY_ARCHIVED_DEFAULT_STR = "false";
  protected static final boolean QUERY_ARCHIVED_DEFAULT = parseBoolean(QUERY_ARCHIVED_DEFAULT_STR);

  public List<ListWorkflowDefinitionResponse> listWorkflowDefinitions(final List<String> types,
      final WorkflowDefinitionService workflowDefinitions, final ListWorkflowDefinitionConverter converter,
      final WorkflowDefinitionDao workflowDefinitionDao) {
    List<AbstractWorkflowDefinition> definitions = workflowDefinitions.getWorkflowDefinitions();
    Set<String> reqTypes = new HashSet<>(types);
    Set<String> foundTypes = new HashSet<>();
    List<ListWorkflowDefinitionResponse> response = new ArrayList<>();
    for (AbstractWorkflowDefinition definition : definitions) {
      if (reqTypes.isEmpty() || reqTypes.contains(definition.getType())) {
        foundTypes.add(definition.getType());
        response.add(converter.convert(definition));
      }
    }
    if (reqTypes.isEmpty() || foundTypes.size() < reqTypes.size()) {
      reqTypes.removeAll(foundTypes);
      List<StoredWorkflowDefinition> storedDefinitions = workflowDefinitionDao.queryStoredWorkflowDefinitions(reqTypes);
      for (StoredWorkflowDefinition storedDefinition : storedDefinitions) {
        if (!foundTypes.contains(storedDefinition.type)) {
          response.add(converter.convert(storedDefinition));
        }
      }
    }
    sort(response);
    return response;
  }

  public boolean updateWorkflowInstance(final long id,
      final UpdateWorkflowInstanceRequest req, final WorkflowInstanceFactory workflowInstanceFactory,
      final WorkflowInstanceService workflowInstances, WorkflowInstanceDao workflowInstanceDao) {
    WorkflowInstance.Builder builder = workflowInstanceFactory.newWorkflowInstanceBuilder().setId(id)
        .setNextActivation(req.nextActivationTime);
    String msg = defaultIfBlank(req.actionDescription, "");
    if (!isEmpty(req.state)) {
      builder.setState(req.state);
      if (isBlank(req.actionDescription)) {
        msg = "API changed state to " + req.state + ". ";
      }
    }
    if (req.nextActivationTime != null && isBlank(req.actionDescription)) {
      msg += "API changed nextActivationTime to " + req.nextActivationTime + ". ";
    }
    if (!req.stateVariables.isEmpty()) {
      for (Entry<String, Object> entry : req.stateVariables.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof String) {
          builder.putStateVariable(entry.getKey(), (String) value);
        } else {
          builder.putStateVariable(entry.getKey(), value);
        }
      }
      if (isBlank(req.actionDescription)) {
        msg += "API updated state variables. ";
      }
    }
    if (!isEmpty(req.businessKey)) {
      builder.setBusinessKey(req.businessKey);
      if (isBlank(req.actionDescription)) {
        msg = "API changed business key to " + req.businessKey + ". ";
      }
    }
    if (msg.isEmpty()) {
      return true;
    }
    WorkflowInstance instance = builder.setStateText(msg).build();
    instance.getChangedStateVariables().forEach(workflowInstanceDao::checkStateVariableValueLength);
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange)
        .setStateText(trimToNull(msg)).setExecutionEnd(now()).build();
    return workflowInstances.updateWorkflowInstance(instance, action);
  }

  public Stream<ListWorkflowInstanceResponse> listWorkflowInstances(List<Long> ids, List<String> types, Long parentWorkflowId,
      Long parentActionId, List<String> states, List<WorkflowInstanceStatus> statuses, String businessKey, String externalId,
      String stateVariableKey, String stateVariableValue, String include, Long maxResults, Long maxActions, boolean queryArchive,
      WorkflowInstanceService workflowInstances, ListWorkflowInstanceConverter listWorkflowConverter) {
    Set<String> includeStrings = parseIncludeStrings(include).collect(toSet());
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder()
        .addIds(ids.toArray(new Long[ids.size()]))
        .addTypes(types.toArray(new String[types.size()]))
        .setParentWorkflowId(parentWorkflowId)
        .setParentActionId(parentActionId)
        .addStates(states.toArray(new String[states.size()]))
        .addStatuses(statuses.toArray(new WorkflowInstanceStatus[statuses.size()]))
        .setBusinessKey(businessKey)
        .setExternalId(externalId)
        .setIncludeCurrentStateVariables(includeStrings.contains(currentStateVariables))
        .setIncludeActions(includeStrings.contains(actions))
        .setIncludeActionStateVariables(includeStrings.contains(actionStateVariables))
        .setMaxResults(maxResults)
        .setMaxActions(maxActions)
        .setQueryArchive(queryArchive)
        .setIncludeChildWorkflows(includeStrings.contains(childWorkflows))
        .setStateVariable(stateVariableKey, stateVariableValue)
        .build();
    Stream<WorkflowInstance> instances = workflowInstances.listWorkflowInstancesAsStream(q);
    Set<WorkflowInstanceInclude> parseIncludeEnums = parseIncludeEnums(include);
    return instances.map(instance -> listWorkflowConverter.convert(instance, parseIncludeEnums));
  }

  private Set<WorkflowInstanceInclude> parseIncludeEnums(String include) {
    return parseIncludeStrings(include).map(INCLUDE_STRING_TO_ENUM::get).filter(Objects::nonNull)
        .collect(toCollection(HashSet::new));
  }

  private Stream<String> parseIncludeStrings(String include) {
    return Stream.of(trimToEmpty(include).split(","));
  }

  public ListWorkflowInstanceResponse fetchWorkflowInstance(final long id, final String include, final Long maxActions, boolean queryArchive,
      final WorkflowInstanceService workflowInstances,
      final ListWorkflowInstanceConverter listWorkflowConverter) throws EmptyResultDataAccessException {
    Set<WorkflowInstanceInclude> includes = parseIncludeEnums(include);
    WorkflowInstance instance = workflowInstances.getWorkflowInstance(id, includes, maxActions, queryArchive);
    return listWorkflowConverter.convert(instance, includes);
  }

  protected int resolveExceptionHttpStatus(Throwable t) {
    if (t instanceof IllegalArgumentException) {
      return HTTP_BAD_REQUEST;
    } else if (t instanceof NflowNotFoundException) {
      return HTTP_NOT_FOUND;
    }
    return HTTP_INTERNAL_ERROR;
  }

  protected <T> T handleExceptions(Supplier<T> response, BiFunction<Integer, ErrorResponse, T> error) {
    try {
      return response.get();
    } catch (Throwable t) {
      int code = resolveExceptionHttpStatus(t);
      return error.apply(code, new ErrorResponse(t.getMessage()));
    }
  }
}
