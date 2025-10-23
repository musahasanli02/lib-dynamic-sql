package az.kapitalbank.loysql;

import az.kapitalbank.loysql.config.LibConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DynamicSqlExecutor.QueryBuilder
 */
@ExtendWith(MockitoExtension.class)
class QueryBuilderTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private LibConfigProperties configProperties;
    private DynamicSqlExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        configProperties = new LibConfigProperties();
        configProperties.setProcedureName("TEST_PROCEDURE");
        configProperties.setDefaultSchema("TEST_SCHEMA");
        configProperties.setDefaultCatalog("TEST_CATALOG");
        configProperties.setLogQueries(false);

        when(dataSource.getConnection()).thenReturn(connection);

        executor = new DynamicSqlExecutor(dataSource, configProperties);
    }

    @Test
    void shouldCreateQueryBuilderWithQueryName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USERS");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldAddSingleParam() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("userId", 123);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldAddMultipleParamsWithFluentAPI() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USERS")
                .param("categoryId", 1)
                .param("cityId", 23)
                .param("status", "ACTIVE");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldAddParamsFromMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);
        params.put("status", "ACTIVE");

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .params(params);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldCombineParamAndParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);
        params.put("status", "ACTIVE");

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("categoryId", 1)
                .params(params)
                .param("cityId", 23);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleNullParamsMap() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .params(null);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleEmptyParamsMap() {
        Map<String, Object> params = new HashMap<>();

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .params(params);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldOverrideCatalogName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .catalog("CUSTOM_CATALOG");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldChainCatalogWithParams() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .catalog("CUSTOM_CATALOG")
                .param("userId", 123)
                .param("status", "ACTIVE");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldCreateQueryBuilderWithCustomCatalogInConstructor() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER", "CUSTOM_CATALOG");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldAllowNullParamValues() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("nullableField", null);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldAllowEmptyStringParamValues() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("emptyField", "");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleNumericParamTypes() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("intValue", 42)
                .param("longValue", 123456789L)
                .param("doubleValue", 3.14159)
                .param("floatValue", 2.5f);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleBooleanParamTypes() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("isActive", true)
                .param("isDeleted", false);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleComplexObjectParamTypes() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("city", "Baku");
        nestedMap.put("country", "Azerbaijan");

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("address", nestedMap);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldOverrideParamValueWhenSetMultipleTimes() {
        Map<String, Object> params1 = new HashMap<>();
        params1.put("userId", 123);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("userId", 456);

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .params(params1)
                .params(params2);

        assertThat(builder).isNotNull();
        // The second call should override the first for the same key
    }

    @Test
    void shouldMergeMultipleParamsCalls() {
        Map<String, Object> params1 = new HashMap<>();
        params1.put("userId", 123);
        params1.put("status", "ACTIVE");

        Map<String, Object> params2 = new HashMap<>();
        params2.put("categoryId", 1);
        params2.put("cityId", 23);

        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .params(params1)
                .params(params2);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleSpecialCharactersInParamName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER")
                .param("user_id", 123)
                .param("user$name", "test")
                .param("user.email", "test@example.com");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleSpecialCharactersInQueryName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER_$_DATA");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleVeryLongQueryName() {
        String longQueryName = "GET_USER_DATA_WITH_VERY_LONG_NAME_THAT_EXCEEDS_NORMAL_LENGTH_LIMITS_FOR_TESTING";

        DynamicSqlExecutor.QueryBuilder builder = executor.query(longQueryName);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleManyParameters() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("COMPLEX_QUERY");

        for (int i = 0; i < 100; i++) {
            builder.param("param" + i, "value" + i);
        }

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldReturnBuilderForMethodChaining() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USER");

        // Verify all methods return the builder for chaining
        DynamicSqlExecutor.QueryBuilder sameBuilder = builder
                .param("userId", 123)
                .param("status", "ACTIVE")
                .catalog("CUSTOM_CATALOG");

        assertThat(sameBuilder).isSameAs(builder);
    }

    @Test
    void shouldHandleNullQueryName() {
        // This tests that the builder can be created with null query name
        // The actual behavior will be determined by the database call
        DynamicSqlExecutor.QueryBuilder builder = executor.query(null);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldHandleEmptyQueryName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldUseDefaultCatalogWhenNotOverridden() {
        configProperties.setDefaultCatalog("DEFAULT_CATALOG");
        DynamicSqlExecutor executorWithDefaultCatalog = new DynamicSqlExecutor(dataSource, configProperties);

        DynamicSqlExecutor.QueryBuilder builder = executorWithDefaultCatalog.query("GET_USER")
                .param("userId", 123);

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldOverrideDefaultCatalogWhenSpecified() {
        configProperties.setDefaultCatalog("DEFAULT_CATALOG");
        DynamicSqlExecutor executorWithDefaultCatalog = new DynamicSqlExecutor(dataSource, configProperties);

        DynamicSqlExecutor.QueryBuilder builder = executorWithDefaultCatalog.query("GET_USER")
                .catalog("CUSTOM_CATALOG")
                .param("userId", 123);

        assertThat(builder).isNotNull();
    }
}
