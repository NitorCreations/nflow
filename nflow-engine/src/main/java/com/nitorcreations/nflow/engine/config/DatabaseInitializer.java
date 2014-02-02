package com.nitorcreations.nflow.engine.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseInitializer {

  public DatabaseInitializer(DataSource ds) {
    ResourceDatabasePopulator populator = populator();
    populator.addScript(new ClassPathResource("scripts/create_nflow_db.sql"));
    execute(populator, ds);
  }
  
  private ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

}
