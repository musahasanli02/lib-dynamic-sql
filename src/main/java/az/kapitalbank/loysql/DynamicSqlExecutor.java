package az.kapitalbank.loysql;

import az.kapitalbank.loysql.config.LibConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized executor for Oracle database stored procedure that accepts JSON.
 * <p>
 * The executor calls a single central procedure with a JSON payload containing:
 * - queryName: The SQL query identifier
 * - params: The parameters for the query
 * <p>
 * Example usage:
 * <pre>
 * List&lt;UserDto&gt; users = executor.query("GET_USERS_LIST")
 *         .param("categoryId", 1)
 *         .param("cityId", 23)
 *         .executeForList(UserDto.class);
 * </pre>
 *
 * @author Kapital Bank
 * @version 1.0.0
 */
public class DynamicSqlExecutor {

    private static final Logger log = LoggerFactory.getLogger(DynamicSqlExecutor.class);

    private final DataSource dataSource;
    private final LibConfigProperties configProperties;
    private final ObjectMapper objectMapper;

    public DynamicSqlExecutor(DataSource dataSource,
                                  LibConfigProperties configProperties) {
        this.dataSource = dataSource;
        this.configProperties = configProperties;
        this.objectMapper = new ObjectMapper();

        log.info("OracleFunctionExecutor initialized - Procedure: {}, Schema: {}",
                configProperties.getProcedureName(), configProperties.getDefaultSchema());
    }

    /**
     * Start building a query execution
     *
     * @param queryName The query identifier (e.g., "GET_USERS_LIST")
     * @return QueryBuilder for fluent API
     */
    public QueryBuilder query(String queryName) {
        return new QueryBuilder(queryName, this);
    }

    /**
     * Start building a query execution with custom catalog
     *
     * @param queryName The query identifier (e.g., "GET_USERS_LIST")
     * @param catalogName The catalog name to use for this query
     * @return QueryBuilder for fluent API
     */
    public QueryBuilder query(String queryName, String catalogName) {
        return new QueryBuilder(queryName, catalogName, this);
    }

    /**
     * Execute query with parameters and return list of results
     *
     * @param queryName The query identifier
     * @param resultClass Expected result class
     * @param params Query parameters
     * @return List of result objects
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> executeForList(String queryName, Class<T> resultClass, Map<String, Object> params) {
        String jsonPayload = createJsonPayload(queryName, params);

        logQueryIfEnabled(queryName, params, jsonPayload);

        SimpleJdbcCall jdbcCall = createJdbcCall();
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("P_JSON", jsonPayload);

        return (List<T>) jdbcCall.executeFunction(resultClass, parameterSource);
    }

    /**
     * Execute query with parameters and return list of results
     *
     * @param queryName The query identifier
     * @param resultClass Expected result class
     * @param params Query parameters
     * @param catalogName name of the catalog
     * @return List of result objects
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> executeForList(
            String queryName,
            Class<T> resultClass,
            Map<String, Object> params,
            String catalogName
    ) {
        String jsonPayload = createJsonPayload(queryName, params);

        logQueryIfEnabled(queryName, params, jsonPayload);

        SimpleJdbcCall jdbcCall = createJdbcCall();
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("P_JSON", jsonPayload);

        return (List<T>) jdbcCall
                .withCatalogName(catalogName)
                .executeFunction(resultClass, parameterSource);
    }

    /**
     * Execute query and return single result
     *
     * @param queryName The query identifier
     * @param resultClass Expected result class
     * @param params Query parameters
     * @return Single result object
     */
    public <T> T executeForObject(String queryName, Class<T> resultClass, Map<String, Object> params) {
        String jsonPayload = createJsonPayload(queryName, params);

        logQueryIfEnabled(queryName, params, jsonPayload);

        SimpleJdbcCall jdbcCall = createJdbcCall();
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("P_JSON", jsonPayload);

        return jdbcCall
                .withCatalogName(configProperties.getDefaultCatalog())
                .executeFunction(resultClass, parameterSource);
    }

    /**
     * Execute query in custom catalog and return single result
     *
     * @param queryName The query identifier
     * @param resultClass Expected result class
     * @param params Query parameters
     * @param catalogName name of the catalog
     * @return Single result object
     */
    public <T> T executeForObject(String queryName, Class<T> resultClass, Map<String, Object> params, String catalogName) {
        String jsonPayload = createJsonPayload(queryName, params);

        logQueryIfEnabled(queryName, params, jsonPayload);

        SimpleJdbcCall jdbcCall = createJdbcCall();
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("P_JSON", jsonPayload);

        return jdbcCall
                .withCatalogName(catalogName)
                .executeFunction(resultClass, parameterSource);
    }

    /**
     * Create the JSON payload for the stored procedure
     */
    private String createJsonPayload(String queryName, Map<String, Object> params) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("queryName", queryName);
        payload.put("params", params != null ? params : new HashMap<>());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to create JSON payload for query: {}", queryName, e);
            throw new RuntimeException("Failed to serialize JSON payload", e);
        }
    }

    /**
     * Create SimpleJdbcCall for the central procedure
     */
    private SimpleJdbcCall createJdbcCall() {
        SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
                .withFunctionName(configProperties.getProcedureName());

        if (configProperties.getDefaultSchema() != null && !configProperties.getDefaultSchema().isEmpty()) {
            call.withSchemaName(configProperties.getDefaultSchema());
        }

        return call;
    }

    /**
     * Log query name and parameters if logging enabled
     */
    private void logQueryIfEnabled(String queryName, Map<String, Object> params, String jsonPayload) {
        if (configProperties.logQueries()) {
            log.info("Executing query: {} with params: {}", queryName, params);
            log.debug("JSON payload: {}", jsonPayload);
        }
    }

    /**
     * Fluent builder for query execution
     */
    public static class QueryBuilder {
        private final String queryName;
        private final DynamicSqlExecutor executor;
        private final Map<String, Object> params;
        private String catalogName;

        private QueryBuilder(String queryName, DynamicSqlExecutor executor) {
            this.queryName = queryName;
            this.executor = executor;
            this.params = new HashMap<>();
            this.catalogName = executor.configProperties.getDefaultCatalog();
        }

        private QueryBuilder(String queryName, String catalogName, DynamicSqlExecutor executor) {
            this.queryName = queryName;
            this.executor = executor;
            this.params = new HashMap<>();
            this.catalogName = catalogName;
        }

        /**
         * Override catalog name for this query
         *
         * @param catalogName Parameter name
         * @return This builder for chaining
         */
        public QueryBuilder catalog(String catalogName) {
            this.catalogName = catalogName;
            return this;
        }

        /**
         * Add a parameter to the query
         *
         * @param name Parameter name
         * @param value Parameter value
         * @return This builder for chaining
         */
        public QueryBuilder param(String name, Object value) {
            params.put(name, value);
            return this;
        }

        /**
         * Add multiple parameters at once
         *
         * @param params Map of parameters
         * @return This builder for chaining
         */
        public QueryBuilder params(Map<String, Object> params) {
            if (params != null) {
                this.params.putAll(params);
            }
            return this;
        }

        /**
         * Execute and return a list of results
         *
         * @param resultClass Expected result class
         * @return List of result objects
         */
        public <T> List<T> executeForList(Class<T> resultClass) {
            return executor.executeForList(queryName, resultClass, params, catalogName);
        }

        /**
         * Execute and return a single result
         *
         * @param resultClass Expected result class
         * @return Single result object
         */
        public <T> T executeForObject(Class<T> resultClass) {
            return executor.executeForObject(queryName, resultClass, params, catalogName);
        }

        /**
         * Execute without expecting a result (for procedures that don't return data)
         */
        public void execute() {
            executor.executeForObject(queryName, Void.class, params, catalogName);
        }
    }
}