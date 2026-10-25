package io.nflow.engine.internal.dao;

import static io.nflow.engine.config.Profiles.H2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.inject.Inject;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ExecutorDaoTransactionSupportTest.ForeignTransactionManagerConfiguration.class)
@ActiveProfiles(H2)
public class ExecutorDaoTransactionSupportTest {

  @Inject
  ExecutorDao dao;

  @Test
  public void transactionSupportIsNotEnabledWhenTransactionManagerDoesNotGovernNflowDataSource() {
    assertThat(dao.isAutoCommitEnabled(), is(true));
    assertThat(dao.isTransactionSupportEnabled(), is(false));
  }

  static class ForeignTransactionManagerConfiguration extends DaoTestConfiguration {
    @Override
    @Bean
    public PlatformTransactionManager transactionManager(DataSource nflowDataSource) {
      DataSource otherDataSource = new DriverManagerDataSource("jdbc:h2:mem:other;DB_CLOSE_DELAY=-1", "sa", "");
      return new DataSourceTransactionManager(otherDataSource);
    }
  }
}
