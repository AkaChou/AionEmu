package com.aionemu.commons.database;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.configs.DatabaseConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Slf4j
final class DatabaseSchemaInitializer {

    private static final Map<String, String> BASELINE_SCHEMAS = Map.of(
        "al_server_gs", "db/mysql/al_server_gs.sql",
        "al_server_ls", "db/mysql/al_server_ls.sql"
    );

    private DatabaseSchemaInitializer() {
    }

    static void initializeIfMissing() {
        initializeIfMissing(DatabaseConfig.DATABASE_URL, DatabaseConfig.DATABASE_USER, DatabaseConfig.DATABASE_PASSWORD);
    }

    static void initializeIfMissing(String jdbcUrl, String user, String password) {
        JdbcTarget target = JdbcTarget.from(jdbcUrl);
        String schemaResource = schemaResource(target.database());
        if (schemaResource == null) {
            log.debug("No bundled baseline schema for database {}; skipping schema initialization.", target.database());
            return;
        }

        try (Connection connection = DriverManager.getConnection(target.serverUrl(), user, password)) {
            if (hasTables(connection, target.database())) {
                log.debug("Database {} already contains tables; skipping schema initialization.", target.database());
                return;
            }

            log.info("Database {} has no tables; initializing schema from {}.", target.database(), schemaResource);
            executeScript(connection, schemaResource);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize database schema for " + target.database(), e);
        }
    }

    static String schemaResource(String database) {
        return BASELINE_SCHEMAS.get(database);
    }

    private static boolean hasTables(Connection connection, String database) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static void executeScript(Connection connection, String schemaResource) throws SQLException, IOException {
        for (String statementSql : splitStatements(readResource(schemaResource))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private static String readResource(String schemaResource) throws IOException {
        ClassLoader classLoader = DatabaseSchemaInitializer.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(schemaResource)) {
            if (inputStream == null) {
                throw new IOException("Missing schema resource " + schemaResource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder script = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("#")) {
                        script.append(line).append('\n');
                    }
                }
                return script.toString();
            }
        }
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(ch);
                continue;
            }
            if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }

        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    record JdbcTarget(String serverUrl, String database) {

        private static final String MYSQL_PREFIX = "jdbc:mysql://";

        static JdbcTarget from(String jdbcUrl) {
            if (jdbcUrl == null || !jdbcUrl.startsWith(MYSQL_PREFIX)) {
                throw new IllegalArgumentException("Only MySQL JDBC URLs are supported for schema initialization: " + jdbcUrl);
            }

            int queryIndex = jdbcUrl.indexOf('?');
            String baseUrl = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
            String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
            int databaseSeparator = baseUrl.indexOf('/', MYSQL_PREFIX.length());
            if (databaseSeparator < 0 || databaseSeparator == baseUrl.length() - 1) {
                throw new IllegalArgumentException("MySQL JDBC URL must include a database name: " + jdbcUrl);
            }

            String database = baseUrl.substring(databaseSeparator + 1);
            int pathParameterIndex = database.indexOf(';');
            if (pathParameterIndex >= 0) {
                database = database.substring(0, pathParameterIndex);
            }
            if (database.isBlank()) {
                throw new IllegalArgumentException("MySQL JDBC URL must include a database name: " + jdbcUrl);
            }

            String serverUrl = baseUrl.substring(0, databaseSeparator + 1) + query;
            return new JdbcTarget(serverUrl, database);
        }
    }
}
