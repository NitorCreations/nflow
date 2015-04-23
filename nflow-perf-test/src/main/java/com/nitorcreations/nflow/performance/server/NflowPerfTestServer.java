package com.nitorcreations.nflow.performance.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.jetty.StartNflow;
import com.nitorcreations.nflow.metrics.NflowMetricsContext;
import com.nitorcreations.nflow.performance.testdata.TestDataBatchInserter;
import com.nitorcreations.nflow.performance.testdata.TestDataGenerator;
import com.nitorcreations.nflow.performance.testdata.TestDataManager;

/**
 * Startup class for performance tested server.
 *
 * java -Dhost=192.168.0.1 -Dnflow.db.user=nflow -Dnflow.db.password=nflow
 * -Dnflow.db.postgresql.url=jdbc:postgresql://nflow-perftest.rds.amazonaws.com:5432/nflow?tcpKeepAlive=true
 * -Dnflow.executor.group=nflow-perf -Dnflow.non_spring_workflows_filename=workflows.txt
 * -Dspring.profiles.active=nflow.db.postgresql -Dgraphite.host=192.168.201.76 -Dgraphite.port=2003 -jar
 * nflow/nflow-perf-test/target/nflow-perf-tests-*-SNAPSHOT.jar
 */
public class NflowPerfTestServer {

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && "generateTestData".equals(args[0])) {
      Map<String, Object> props = new HashMap<>();
      props.put("nflow.autostart", "false");
      new StartNflow().registerSpringContext(TestDataGeneratorConfig.class).startJetty(props);
    } else {
      new StartNflow().registerSpringContext(NflowMetricsContext.class).startJetty(Collections.<String, Object> emptyMap());
    }
  }

  @Configuration
  public static class TestDataGeneratorConfig {
    @Bean
    public TestDataGenerator testDataGenerator(ExecutorDao executors, WorkflowDefinitionService workflowDefinitions) {
      return new TestDataGenerator(executors, workflowDefinitions);
    }

    @Bean
    public TestDataBatchInserter testDataBatchInserter(JdbcTemplate jdbcTemplate, SQLVariants sqlVariants) {
      return new TestDataBatchInserter(jdbcTemplate, sqlVariants);
    }

    @Bean
    public TestDataManager testDataManager(TestDataGenerator generator, TestDataBatchInserter inserter, Environment env) {
      return new TestDataManager(generator, inserter, env);
    }
  }

}
