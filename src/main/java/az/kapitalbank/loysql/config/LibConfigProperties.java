package az.kapitalbank.loysql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the dynamic sql library.
 *
 * These properties can be configured in application.yml or application.properties:
 *
 * <pre>
 * loysql:
 *   enabled: true
 *   default-schema: MY_SCHEMA
 *   procedureName: DYNAMIC_SQL_EXECUTOR
 *   log-queries: false
 *   timeout: 30
 * </pre>
 *
 * @author Musa Hasanli
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "loysql")
public class LibConfigProperties {

    /**
     * Enable/disable Oracle function executor.
     * If set to false, the executor bean will not be created.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Default schema name for Oracle functions.
     * If specified, all function calls will use this schema unless overridden.
     * Example: "LOYALTY_SCHEMA"
     * Default: null (uses default schema from database connection)
     */
    private String defaultSchema;

    /**
     * Name of the default stored procedure which accepts query name and params and executes related sql.
     * Example: "DYNAMIC_SQL_EXECUTOR"
     * Default: "EXECUTE_DYNAMIC_SQL"
     */
    private String procedureName = "EXECUTE_DYNAMIC_SQL";

    /**
     * Enable detailed query logging.
     * When enabled, logs function names and parameters before execution.
     * Useful for debugging but may impact performance.
     * Default: false
     */
    private boolean logQueries = false;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public boolean logQueries() {
        return logQueries;
    }

    public void setLogQueries(boolean logQueries) {
        this.logQueries = logQueries;
    }

    @Override
    public String toString() {
        return "OracleExecutorProperties{" +
                "enabled=" + enabled +
                ", defaultSchema='" + defaultSchema + '\'' +
                ", logQueries=" + logQueries + '\'' +
                ", procedureName='" + procedureName + '\'' +
                '}';
    }
}