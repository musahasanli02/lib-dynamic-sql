package az.kapitalbank.loysql.config;

import az.kapitalbank.loysql.DynamicSqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Autoconfiguration for Dynamic SQL Library
 *
 * This configuration will automatically create an LibAutoConfiguration bean
 * when the following conditions are met:
 * - DataSource and SimpleJdbcCall classes are present on the classpath
 * - loysql.enabled property is true (or not set, defaults to true)
 *
 * @author Musa Hasanli
 * @version 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, SimpleJdbcCall.class})
@ConditionalOnProperty(
        prefix = "loysql",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(LibConfigProperties.class)
public class LibAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LibAutoConfiguration.class);

    /**
     * Creates the main OracleFunctionExecutor bean
     *
     * @param dataSource The DataSource to use for database connections
     * @param properties Configuration properties for the executor
     * @return Configured OracleFunctionExecutor instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamicSqlExecutor dynamicSqlService(
            DataSource dataSource,
            LibConfigProperties properties) {

        log.info("Initializing Oracle Function Executor");

        if (!StringUtils.hasText(properties.getProcedureName())) {
            throw new IllegalStateException(
                    "Oracle executor is enabled but 'loysql.procedure-name' is not configured. " +
                            "Please set the procedure name in your application.yml or application.properties"
            );
        }

        log.debug("Configuration - Default Schema: {}, Log Queries: {}",
                properties.getDefaultSchema(),
                properties.logQueries());

        DynamicSqlExecutor executor = new DynamicSqlExecutor(dataSource, properties);

        log.info("Oracle Function Executor initialized successfully");
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        return new DriverManagerDataSource();
    }

}
