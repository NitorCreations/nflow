package com.nitorcreations.nflow.engine.db;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Profile("nflow.db.h2")
@Configuration
public class H2DatabaseConfiguration extends DatabaseConfiguration {
  public H2DatabaseConfiguration() {
    super("h2");
  }

  @Bean(initMethod="start", destroyMethod="stop")
  Server h2TcpServer(Environment env) throws SQLException {
    String port = env.getProperty("nflow.db.h2.tcp.port");
    if (isBlank(port)) {
      return null;
    }
    return Server.createTcpServer("-tcp","-tcpAllowOthers","-tcpPort",port);
  }

  @Bean(initMethod="start", destroyMethod="stop")
  Server h2ConsoleServer(Environment env) throws SQLException {
    String port = env.getProperty("nflow.db.h2.console.port");
    if (isBlank(port)) {
      return null;
    }
    return Server.createTcpServer("-webPort",port);
  }
}
