package az.kapitalbank.loysql.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LibConfigProperties
 */
class LibConfigPropertiesTest {

    private LibConfigProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LibConfigProperties();
    }

    @Test
    void shouldHaveDefaultValues() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDefaultSchema()).isNull();
        assertThat(properties.getDefaultCatalog()).isNull();
        assertThat(properties.getProcedureName()).isEqualTo("EXECUTE_DYNAMIC_SQL");
        assertThat(properties.logQueries()).isFalse();
    }

    @Test
    void shouldSetAndGetEnabled() {
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();

        properties.setEnabled(true);
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldSetAndGetDefaultSchema() {
        String schema = "TEST_SCHEMA";
        properties.setDefaultSchema(schema);
        assertThat(properties.getDefaultSchema()).isEqualTo(schema);
    }

    @Test
    void shouldSetAndGetDefaultCatalog() {
        String catalog = "TEST_CATALOG";
        properties.setDefaultCatalog(catalog);
        assertThat(properties.getDefaultCatalog()).isEqualTo(catalog);
    }

    @Test
    void shouldSetAndGetProcedureName() {
        String procedureName = "MY_CUSTOM_PROCEDURE";
        properties.setProcedureName(procedureName);
        assertThat(properties.getProcedureName()).isEqualTo(procedureName);
    }

    @Test
    void shouldSetAndGetLogQueries() {
        properties.setLogQueries(true);
        assertThat(properties.logQueries()).isTrue();

        properties.setLogQueries(false);
        assertThat(properties.logQueries()).isFalse();
    }

    @Test
    void shouldGenerateCorrectToString() {
        properties.setEnabled(true);
        properties.setDefaultSchema("MY_SCHEMA");
        properties.setProcedureName("MY_PROC");
        properties.setLogQueries(true);

        String result = properties.toString();

        assertThat(result).contains("enabled=true");
        assertThat(result).contains("defaultSchema='MY_SCHEMA'");
        assertThat(result).contains("logQueries=true");
        assertThat(result).contains("procedureName='MY_PROC'");
    }

    @Test
    void shouldHandleNullValuesInSetters() {
        properties.setDefaultSchema(null);
        properties.setDefaultCatalog(null);
        properties.setProcedureName(null);

        assertThat(properties.getDefaultSchema()).isNull();
        assertThat(properties.getDefaultCatalog()).isNull();
        assertThat(properties.getProcedureName()).isNull();
    }

    @Test
    void shouldHandleEmptyStringsInSetters() {
        properties.setDefaultSchema("");
        properties.setDefaultCatalog("");
        properties.setProcedureName("");

        assertThat(properties.getDefaultSchema()).isEmpty();
        assertThat(properties.getDefaultCatalog()).isEmpty();
        assertThat(properties.getProcedureName()).isEmpty();
    }
}
