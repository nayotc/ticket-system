package ticketsystem.PersistenceTesting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the cloud database configuration.
 *
 * The positive test intentionally connects to the real cloud database, but only
 * executes "SELECT 1", so it verifies connectivity without changing or polluting
 * production/project data.
 *
 * To avoid failing every local build when the cloud DB is unavailable, the real
 * connection test runs only when RUN_CLOUD_DB_TESTS=true is defined.
 */
class CloudDatabaseConnectionTest {

    /**
     * Positive cloud DB test:
     * verifies that application-cloud.properties can be resolved from environment
     * variables and that the remote PostgreSQL database is reachable.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_CLOUD_DB_TESTS", matches = "true")
    void GivenValidCloudDatabaseConfiguration_WhenOpeningConnection_ThenCloudDatabaseIsReachable()
            throws Exception {

        Properties properties = loadProperties("application-cloud.properties");

        String url = resolveRequiredPlaceholders(
                properties.getProperty("spring.datasource.url"),
                System.getenv()
        );
        String username = resolveRequiredPlaceholders(
                properties.getProperty("spring.datasource.username"),
                System.getenv()
        );
        String password = resolveRequiredPlaceholders(
                properties.getProperty("spring.datasource.password"),
                System.getenv()
        );
        String driver = properties.getProperty("spring.datasource.driver-class-name");

        assertEquals("org.postgresql.Driver", driver);
        assertTrue(url.startsWith("jdbc:postgresql://"),
                "Cloud datasource URL should use PostgreSQL");

        Class.forName(driver);

        try (Connection connection = DriverManager.getConnection(url, username, password);
             ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1")) {

            assertTrue(connection.isValid(5),
                    "Cloud database connection should be valid");
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }

    /**
     * Negative cloud config test:
     * verifies that the cloud configuration cannot be resolved without required
     * environment variables. This prevents accidental usage of hard-coded secrets.
     */
    @Test
    void GivenCloudConfigurationWithoutEnvironmentVariables_WhenResolvingDatasource_ThenConfigurationIsInvalid()
            throws IOException {

        Properties properties = loadProperties("application-cloud.properties");

        Map<String, String> emptyEnvironment = Map.of();

        assertThrows(IllegalStateException.class,
                () -> resolveRequiredPlaceholders(
                        properties.getProperty("spring.datasource.url"),
                        emptyEnvironment
                ));

        assertThrows(IllegalStateException.class,
                () -> resolveRequiredPlaceholders(
                        properties.getProperty("spring.datasource.username"),
                        emptyEnvironment
                ));

        assertThrows(IllegalStateException.class,
                () -> resolveRequiredPlaceholders(
                        properties.getProperty("spring.datasource.password"),
                        emptyEnvironment
                ));
    }

    /**
         * Positive default DB configuration test:
         * verifies that the default application.properties uses an in-memory H2 database
         * and that the configured database connection can actually be opened.
         *
         * This test is safe to run in every PR because it does not connect to the cloud
         * database and does not modify any shared data.
         */
        @Test
        void GivenDefaultApplicationConfiguration_WhenOpeningH2Connection_ThenDatabaseIsReachable()
                throws Exception {

        Properties properties = loadProperties("application.properties");

        String url = properties.getProperty("spring.datasource.url");
        String username = properties.getProperty("spring.datasource.username");
        String password = properties.getProperty("spring.datasource.password", "");
        String driver = properties.getProperty("spring.datasource.driver-class-name");

        assertEquals("org.h2.Driver", driver,
                "Default application configuration should use the H2 driver");

        assertNotNull(url, "Default datasource URL must be defined");
        assertTrue(url.startsWith("jdbc:h2:mem:"),
                "Default datasource should use an in-memory H2 database");

        assertEquals("sa", username,
                "Default H2 username should be sa");

        Class.forName(driver);

        try (Connection connection = DriverManager.getConnection(url, username, password);
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1")) {

                assertTrue(connection.isValid(5),
                        "Default H2 database connection should be valid");

                assertTrue(resultSet.next());
                assertEquals(1, resultSet.getInt(1));
        }
        }

    private static Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = CloudDatabaseConnectionTest.class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {

            assertNotNull(inputStream,
                    "Missing configuration file: " + resourceName);

            properties.load(inputStream);
        }

        return properties;
    }

    /**
     * Resolves placeholders in the format ${ENV_NAME} or ${ENV_NAME:defaultValue}.
     */
    private static String resolveRequiredPlaceholders(String value, Map<String, String> environment) {
        assertNotNull(value, "Property value must not be null");

        Pattern pattern = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");
        Matcher matcher = pattern.matcher(value);
        StringBuffer resolved = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String defaultValue = matcher.group(2);

            String replacement = environment.get(variableName);

            if (replacement == null || replacement.isBlank()) {
                replacement = defaultValue;
            }

            if (replacement == null || replacement.isBlank()) {
                throw new IllegalStateException(
                        "Missing required environment variable: " + variableName
                );
            }

            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(resolved);
        return resolved.toString();
    }
}