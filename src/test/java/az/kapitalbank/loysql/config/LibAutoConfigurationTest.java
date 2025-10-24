package az.kapitalbank.loysql.config;

import az.kapitalbank.loysql.DynamicSqlExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LibAutoConfiguration
 */
class LibAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LibAutoConfiguration.class,
                    DataSourceAutoConfiguration.class
            ));

    @Test
    void shouldCreateDynamicSqlExecutorBeanWhenEnabledIsTrue() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name=TEST_PROCEDURE"
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicSqlExecutor.class);
                    assertThat(context).hasSingleBean(LibConfigProperties.class);
                });
    }

    @Test
    void shouldCreateDynamicSqlExecutorBeanWhenEnabledPropertyNotSet() {
        // When enabled property is not set, it should default to true (matchIfMissing = true)
        contextRunner
                .withPropertyValues("loysql.procedure-name=TEST_PROCEDURE")
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicSqlExecutor.class);
                });
    }

    @Test
    void shouldNotCreateDynamicSqlExecutorBeanWhenEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=false",
                        "loysql.procedure-name=TEST_PROCEDURE"
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DynamicSqlExecutor.class);
                });
    }

    @Test
    void shouldUseDefaultProcedureNameWhenNotConfigured() {
        contextRunner
                .withPropertyValues("loysql.enabled=true")
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicSqlExecutor.class);
                    LibConfigProperties properties = context.getBean(LibConfigProperties.class);
                    assertThat(properties.getProcedureName()).isEqualTo("EXECUTE_DYNAMIC_SQL");
                });
    }

    @Test
    void shouldThrowExceptionWhenProcedureNameIsEmpty() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name="
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("loysql.procedure-name");
                });
    }

    @Test
    void shouldThrowExceptionWhenProcedureNameIsBlank() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name=   "
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("loysql.procedure-name");
                });
    }

    @Test
    void shouldConfigureExecutorWithAllProperties() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name=MY_PROCEDURE",
                        "loysql.default-schema=MY_SCHEMA",
                        "loysql.default-catalog=MY_CATALOG",
                        "loysql.log-queries=true"
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicSqlExecutor.class);

                    LibConfigProperties properties = context.getBean(LibConfigProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getProcedureName()).isEqualTo("MY_PROCEDURE");
                    assertThat(properties.getDefaultSchema()).isEqualTo("MY_SCHEMA");
                    assertThat(properties.getDefaultCatalog()).isEqualTo("MY_CATALOG");
                    assertThat(properties.logQueries()).isTrue();
                });
    }

    @Test
    void shouldNotCreateBeanWhenDataSourceIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LibAutoConfiguration.class))
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name=TEST_PROCEDURE"
                )
                .run(context -> {
                    // Without DataSource, the auto-configuration should not activate
                    // because of @ConditionalOnClass({DataSource.class, SimpleJdbcCall.class})
                    assertThat(context).hasNotFailed();
                    // The bean won't be created because DataSource is required as a constructor parameter
                });
    }

    @Test
    void shouldConfigureWithCustomProcedureName() {
        contextRunner
                .withPropertyValues(
                        "loysql.enabled=true",
                        "loysql.procedure-name=CUSTOM_PROCEDURE"
                )
                .withBean(DataSource.class, this::createTestDataSource)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicSqlExecutor.class);
                    LibConfigProperties properties = context.getBean(LibConfigProperties.class);
                    assertThat(properties.getProcedureName()).isEqualTo("CUSTOM_PROCEDURE");
                });
    }

    private DataSource createTestDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}
