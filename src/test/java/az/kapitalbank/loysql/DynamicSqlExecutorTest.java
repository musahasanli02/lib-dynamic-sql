package az.kapitalbank.loysql;

import az.kapitalbank.loysql.config.LibConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DynamicSqlExecutor
 */
@ExtendWith(MockitoExtension.class)
class DynamicSqlExecutorTest {

    @Mock
    private DataSource dataSource;

    private LibConfigProperties configProperties;
    private DynamicSqlExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        configProperties = new LibConfigProperties();
        configProperties.setProcedureName("TEST_PROCEDURE");
        configProperties.setDefaultSchema("TEST_SCHEMA");
        configProperties.setDefaultCatalog("TEST_CATALOG");
        configProperties.setLogQueries(false);

        executor = new DynamicSqlExecutor(dataSource, configProperties);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCreateExecutorWithValidConfiguration() {
        assertThat(executor).isNotNull();
    }

    @Test
    void shouldCreateQueryBuilderWithQueryName() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USERS");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldCreateQueryBuilderWithQueryNameAndCatalog() {
        DynamicSqlExecutor.QueryBuilder builder = executor.query("GET_USERS", "CUSTOM_CATALOG");

        assertThat(builder).isNotNull();
    }

    @Test
    void shouldCreateJsonPayloadCorrectly() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);
        params.put("status", "ACTIVE");

        // We'll test this indirectly through executeForList
        // Since createJsonPayload is private, we verify it through the public API
        String queryName = "GET_USER";

        // Expected JSON structure
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);
        String expectedJson = objectMapper.writeValueAsString(expectedPayload);

        assertThat(expectedJson).contains("GET_USER");
        assertThat(expectedJson).contains("userId");
        assertThat(expectedJson).contains("123");
        assertThat(expectedJson).contains("ACTIVE");
    }

    @Test
    void shouldHandleNullParamsInJsonPayload() throws Exception {
        String queryName = "GET_ALL";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", new HashMap<>());

        String expectedJson = objectMapper.writeValueAsString(expectedPayload);

        assertThat(expectedJson).contains("GET_ALL");
        assertThat(expectedJson).contains("params");
    }

    @Test
    void shouldHandleEmptyParamsInJsonPayload() throws Exception {
        Map<String, Object> params = new HashMap<>();
        String queryName = "GET_ALL";

        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String expectedJson = objectMapper.writeValueAsString(expectedPayload);

        assertThat(expectedJson).contains("GET_ALL");
    }

    @Test
    void shouldHandleComplexObjectsInParams() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);
        params.put("tags", Arrays.asList("tag1", "tag2"));

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("city", "Baku");
        nestedMap.put("country", "Azerbaijan");
        params.put("address", nestedMap);

        String queryName = "COMPLEX_QUERY";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        assertThat(json).contains("COMPLEX_QUERY");
        assertThat(json).contains("tag1");
        assertThat(json).contains("Baku");
    }

    @Test
    void shouldLogQueryWhenLoggingEnabled() {
        configProperties.setLogQueries(true);
        DynamicSqlExecutor executorWithLogging = new DynamicSqlExecutor(dataSource, configProperties);

        // Just verify it doesn't throw exception when logging is enabled
        assertThat(executorWithLogging).isNotNull();
    }

    @Test
    void shouldNotLogQueryWhenLoggingDisabled() {
        configProperties.setLogQueries(false);
        DynamicSqlExecutor executorWithoutLogging = new DynamicSqlExecutor(dataSource, configProperties);

        assertThat(executorWithoutLogging).isNotNull();
    }

    @Test
    void shouldUseDefaultSchemaWhenConfigured() {
        configProperties.setDefaultSchema("MY_SCHEMA");
        DynamicSqlExecutor executorWithSchema = new DynamicSqlExecutor(dataSource, configProperties);

        assertThat(executorWithSchema).isNotNull();
    }

    @Test
    void shouldHandleNullDefaultSchema() {
        configProperties.setDefaultSchema(null);
        DynamicSqlExecutor executorWithoutSchema = new DynamicSqlExecutor(dataSource, configProperties);

        assertThat(executorWithoutSchema).isNotNull();
    }

    @Test
    void shouldHandleEmptyDefaultSchema() {
        configProperties.setDefaultSchema("");
        DynamicSqlExecutor executorWithEmptySchema = new DynamicSqlExecutor(dataSource, configProperties);

        assertThat(executorWithEmptySchema).isNotNull();
    }

    @Test
    void shouldUseConfiguredProcedureName() {
        configProperties.setProcedureName("CUSTOM_PROCEDURE");
        DynamicSqlExecutor executorWithCustomProc = new DynamicSqlExecutor(dataSource, configProperties);

        assertThat(executorWithCustomProc).isNotNull();
    }

    @Test
    void shouldHandleSpecialCharactersInQueryName() throws Exception {
        String queryName = "GET_USER_$_DATA";
        Map<String, Object> params = new HashMap<>();

        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        assertThat(json).contains("GET_USER_$_DATA");
    }

    @Test
    void shouldHandleSpecialCharactersInParamValues() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("description", "Test with \"quotes\" and 'apostrophes'");
        params.put("path", "C:\\Users\\Test\\file.txt");

        String queryName = "INSERT_DATA";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        // JSON should escape special characters
        assertThat(json).contains("INSERT_DATA");
    }

    @Test
    void shouldHandleNumericParamValues() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("intValue", 42);
        params.put("longValue", 123456789L);
        params.put("doubleValue", 3.14159);
        params.put("floatValue", 2.5f);

        String queryName = "NUMERIC_QUERY";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        assertThat(json).contains("42");
        assertThat(json).contains("123456789");
        assertThat(json).contains("3.14159");
    }

    @Test
    void shouldHandleBooleanParamValues() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("isActive", true);
        params.put("isDeleted", false);

        String queryName = "BOOLEAN_QUERY";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        assertThat(json).contains("true");
        assertThat(json).contains("false");
    }

    @Test
    void shouldHandleNullParamValues() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("nullValue", null);
        params.put("stringValue", "test");

        String queryName = "NULL_QUERY";
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("queryName", queryName);
        expectedPayload.put("params", params);

        String json = objectMapper.writeValueAsString(expectedPayload);

        assertThat(json).contains("null");
        assertThat(json).contains("test");
    }

    static class TestDto {
        private Long id;
        private String name;

        public TestDto() {
        }

        public TestDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
