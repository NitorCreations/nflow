package com.nitorcreations.nflow.engine.internal.storage.db;

import static java.lang.String.format;
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

  @Bean
  public SQLVariants sqlVariants() {
    return new H2SQLVariants();
  }

  public static class H2SQLVariants implements SQLVariants {
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "dateadd('second', " + seconds + ", current_timestamp)";
    }

    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    @Override
    public String castToEnumType(String variable, String type) {
      return variable;
    }

    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    @Override
    public String least(String value1, String value2) {
      return format("(case " +
                      "when %1$s is null then %2$s " +
                      "when %2$s is null then %1$s " +
                      "when %1$s < %2$s then %1$s " +
                      "else %2$s end)",
              value1, value2);
    }

    @Override
    public String least1Param(String value1, String value2) {
      return least(value1, value2);
    }
  }
}
