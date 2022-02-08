package io.nflow.engine.internal.dao;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition.Signal;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

public class WorkflowDefinitionDao {

  private static final Logger logger = getLogger(WorkflowDefinitionDao.class);
  private final ExecutorDao executorInfo;
  private final NamedParameterJdbcTemplate namedJdbc;
  private final ObjectMapper nflowObjectMapper;
  private final SQLVariants sqlVariants;

  public WorkflowDefinitionDao(SQLVariants sqlVariants,
                               NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate,
                               ObjectMapper nflowObjectMapper,
                               ExecutorDao executorDao) {
    this.sqlVariants = sqlVariants;
    this.namedJdbc = nflowNamedParameterJdbcTemplate;
    this.nflowObjectMapper = nflowObjectMapper;
    this.executorInfo = executorDao;
  }

  public void storeWorkflowDefinition(WorkflowDefinition definition) {
    StoredWorkflowDefinition storedDefinition = convert(definition);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("type", definition.getType());
    String serializedDefinition = serializeDefinition(storedDefinition);
    params.addValue("definition_sha1", sha1(serializedDefinition));
    params.addValue("definition", serializedDefinition, sqlVariants.longTextType());
    params.addValue("modified_by", executorInfo.getExecutorId());
    params.addValue("executor_group", executorInfo.getExecutorGroup());

    String sql = "update nflow_workflow_definition "
        + "set definition = :definition, modified_by = :modified_by, definition_sha1 = :definition_sha1 "
        + "where type = :type and executor_group = :executor_group and definition_sha1 <> :definition_sha1";
    int updatedRows = namedJdbc.update(sql, params);
    if (updatedRows == 0) {
      sql = "insert into nflow_workflow_definition(type, definition_sha1, definition, modified_by, executor_group) "
          + "values (:type, :definition_sha1, :definition, :modified_by, :executor_group)";
      try {
        namedJdbc.update(sql, params);
      } catch (DataIntegrityViolationException dex) {
        logger.debug("Another executor already stored the definition.", dex);
      }
    }
  }

  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "exception message is fine")
  private String sha1(String serializedDefinition) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(serializedDefinition.getBytes(UTF_8));
      return format("%040x", new BigInteger(1, digest.digest()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 not supported", e);
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

  StoredWorkflowDefinition convert(WorkflowDefinition definition) {
    StoredWorkflowDefinition resp = new StoredWorkflowDefinition();
    resp.type = definition.getType();
    resp.description = definition.getDescription();
    resp.onError = definition.getErrorState().name();
    Map<String, StoredWorkflowDefinition.State> states = definition.getStates().stream().collect(toMap(WorkflowState::name,
        state -> new StoredWorkflowDefinition.State(state.name(), state.getType().name(), state.getDescription())));
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
    resp.supportedSignals = definition.getSupportedSignals().entrySet().stream().map(entry -> {
      Signal signal = new Signal();
      signal.value = entry.getKey();
      signal.description = entry.getValue();
      return signal;
    }).sorted().collect(toList());
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
