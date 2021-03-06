package ru.hh.nab.datasource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import java.util.Properties;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_SAMPLED_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_NAME;

public class DataSourceFactoryTest {
  private static final String TEST_DATA_SOURCE_TYPE = DataSourceType.MASTER;

  private static EmbeddedPostgres testDb;
  private static DataSourceFactory dataSourceFactory;

  @BeforeClass
  public static void setUpClass() {
    testDb = EmbeddedPostgresDataSourceFactory.getEmbeddedPostgres();
    dataSourceFactory = new DataSourceFactory(new NabMetricsTrackerFactoryProvider(TEST_SERVICE_NAME, mock(StatsDSender.class)));
  }

  @Test
  public void testCreateDataSourceWithIncompleteSettings() {
    Properties properties = createIncompleteTestProperties();

    try {
      createTestDataSource(properties);
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("master.pool"));
    }
  }

  @Test
  public void testCreateDataSource() {
    Properties properties = createTestProperties();

    HikariDataSource dataSource = (HikariDataSource) createTestDataSource(properties);
    assertEquals(TEST_DATA_SOURCE_TYPE, dataSource.getPoolName());
  }

  @Test
  public void testCreateStatementTimeoutDataSource() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(STATEMENT_TIMEOUT_MS), "100");

    assertTrue(createTestDataSource(properties) instanceof StatementTimeoutDataSource);
  }

  @Test
  public void testCreateDataSourceWithMetrics() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(MONITORING_SEND_STATS), "true");
    properties.setProperty(getProperty(MONITORING_LONG_CONNECTION_USAGE_MS), "10");
    properties.setProperty(getProperty(MONITORING_SEND_SAMPLED_STATS), "true");

    HikariDataSource dataSource = (HikariDataSource) createTestDataSource(properties);
    assertNotNull(dataSource.getMetricsTrackerFactory());
  }

  private static DataSource createTestDataSource(Properties properties) {
    return dataSourceFactory.create(TEST_DATA_SOURCE_TYPE, false, new FileSettings(properties));
  }

  private static Properties createTestProperties() {
    Properties properties = createIncompleteTestProperties();
    properties.setProperty(getProperty(DataSourceSettings.POOL_SETTINGS_PREFIX + ".maximumPoolSize"), "2");
    return properties;
  }

  private static Properties createIncompleteTestProperties() {
    Properties properties = new Properties();

    final StringSubstitutor jdbcUrlParamsSubstitutor = new StringSubstitutor(Map.of(
            "port", testDb.getPort(),
            "host", "localhost",
            "user", EmbeddedPostgresDataSourceFactory.DEFAULT_USER
    ));
    properties.setProperty(getProperty(DataSourceSettings.JDBC_URL),
      jdbcUrlParamsSubstitutor.replace(EmbeddedPostgresDataSourceFactory.DEFAULT_JDBC_URL));

    properties.setProperty(getProperty(DataSourceSettings.USER), EmbeddedPostgresDataSourceFactory.DEFAULT_USER);
    properties.setProperty(getProperty(DataSourceSettings.PASSWORD), EmbeddedPostgresDataSourceFactory.DEFAULT_USER);
    return properties;
  }

  private static String getProperty(String propertyName) {
    return String.format("%s.%s", TEST_DATA_SOURCE_TYPE, propertyName);
  }

  private static String getMetricName(String metricName) {
    return String.format("%s.%s.%s", TEST_SERVICE_NAME, TEST_DATA_SOURCE_TYPE, metricName);
  }
}
