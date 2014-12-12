package com.nitorcreations.nflow.engine.internal.dao;

import static java.util.Collections.sort;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

@Component
public class WorkflowDefinitionDao {

  private static final Logger logger = getLogger(WorkflowDefinitionDao.class);
  private ExecutorDao executorInfo;
  private NamedParameterJdbcTemplate namedJdbc;
  private ObjectMapper nflowObjectMapper;

  @Inject
  public void setExecutorDao(ExecutorDao executorDao) {
    this.executorInfo = executorDao;
  }

  @Inject
  public void setNamedParameterJdbcTemplate(@NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate) {
    this.namedJdbc = nflowNamedParameterJdbcTemplate;
  }

  @Inject
  public void setObjectMapper(@NFlow ObjectMapper nflowObjectMapper) {
    this.nflowObjectMapper = nflowObjectMapper;
  }

  public void storeWorkflowDefinition(WorkflowDefinition<? extends WorkflowState> definition) {
    StoredWorkflowDefinition storedDefinition = convert(definition);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("type", definition.getType());
    params.addValue("definition", serializeDefinition(storedDefinition));
    params.addValue("modified_by", executorInfo.getExecutorId());
    params.addValue("executor_group", executorInfo.getExecutorGroup());
    String sql = "update nflow_workflow_definition "
        + "set definition = :definition, modified_by = :modified_by "
        + "where type = :type and executor_group = :executor_group and definition <> :definition";
    int updatedRows = namedJdbc.update(sql, params);
    if (updatedRows == 0) {
      sql = "insert into nflow_workflow_definition(type, definition, modified_by, executor_group) "
          + "values (:type, :definition, :modified_by, :executor_group)";
      try {
        namedJdbc.update(sql, params);
      } catch (DataIntegrityViolationException dex) {
        logger.debug("Another executor already stored the definition.", dex);
      }
    }
  }

  public List<StoredWorkflowDefinition> queryStoredWorkflowDefinitions(Collection<String> types) {
    String sql = "select definition from nflow_workflow_definition where " + executorInfo.getExecutorGroupCondition();
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (!isEmpty(types)) {
      sql += " and type in (:types)";
      params.addValue("types", types);
    }
    return namedJdbc.query(sql, params, new RowMapper<StoredWorkflowDefinition>() {
      @Override
      public StoredWorkflowDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
        return deserializeDefinition(rs.getString("definition"));
      }
    });
  }

  StoredWorkflowDefinition convert(WorkflowDefinition<? extends WorkflowState> definition) {
    StoredWorkflowDefinition resp = new StoredWorkflowDefinition();
    resp.type = definition.getType();
    resp.description = definition.getDescription();
    resp.onError = definition.getErrorState().name();
    Map<String, StoredWorkflowDefinition.State> states = new HashMap<>();
    for (WorkflowState state : definition.getStates()) {
      states.put(state.name(), new StoredWorkflowDefinition.State(state.name(), state.getType().name(), state.getDescription()));
    }
    for (Entry<String, List<String>> entry : definition.getAllowedTransitions().entrySet()) {
      StoredWorkflowDefinition.State state = states.get(entry.getKey());
      for (String targetState : entry.getValue()) {
        state.transitions.add(targetState);
      }
      sort(state.transitions);
    }
    for (Entry<String, WorkflowState> entry : definition.getFailureTransitions().entrySet()) {
      StoredWorkflowDefinition.State state = states.get(entry.getKey());
      state.onFailure = entry.getValue().name();
    }
    resp.states = new ArrayList<>(states.values());
    sort(resp.states);
    return resp;
  }

  private String serializeDefinition(StoredWorkflowDefinition storedDefinition) {
    try {
      return nflowObjectMapper.writeValueAsString(storedDefinition);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize workflow definition " + storedDefinition.type, e);
    }
  }

  StoredWorkflowDefinition deserializeDefinition(String serializedDefinition) {
    try {
      return nflowObjectMapper.readValue(serializedDefinition, StoredWorkflowDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize workflow definition " + serializedDefinition, e);
    }
  }
}
