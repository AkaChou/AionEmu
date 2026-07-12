package com.aionemu.commons.database;


import com.aionemu.boot.i18n.I18n;
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

/**
 * 数据库 schema 初始化器，在空库时导入内置基线 SQL
 * Database schema initializer that imports bundled baseline SQL for empty databases
 */
@Slf4j
final class DatabaseSchemaInitializer {

    private static final Map<String, String> BASELINE_SCHEMAS = Map.of(
        "al_server_gs", "db/mysql/al_server_gs.sql",
        "al_server_ls", "db/mysql/al_server_ls.sql"
    );

    private DatabaseSchemaInitializer() {
    }

    /**
     * 使用当前 DatabaseConfig 在缺失表时初始化 schema
     * Initialize schema when tables are missing using the current DatabaseConfig
     */
    static void initializeIfMissing() {
        initializeIfMissing(DatabaseConfig.DATABASE_URL, DatabaseConfig.DATABASE_USER, DatabaseConfig.DATABASE_PASSWORD);
    }

    /**
     * 使用指定 JDBC 连接信息在缺失表时初始化 schema
     * Initialize schema when tables are missing using the given JDBC settings
     *
     * JDBC URL
     * Username
     * Password
     */
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

            log.info(I18n.get("log.fe7ca44cd941", target.database(), schemaResource));
            executeScript(connection, schemaResource);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize database schema for " + target.database(), e);
        }
    }

    /**
     * 根据库名解析内置基线 schema 资源路径
     * Resolve the bundled baseline schema resource path by database name
     *
     * Database name
     *
     * @param database
     * @return 资源路径，未配置时返回 null / Resource path, or null when not configured
     */
    static String schemaResource(String database) {
        return BASELINE_SCHEMAS.get(database);
    }

    /**
     * 判断指定 schema 是否已有表
     * Check whether the given schema already contains tables
     *
     * @param connection 服务器级连接 / Server-level connection
     * Database name
     *
     * @param connection
     * @return 已有表返回 true / True when tables exist
     * @param database
     * @throws SQLException 查询失败时 / When the query fails
     */
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

    /**
     * 执行资源中的 SQL 脚本
     * Execute the SQL script from a classpath resource
     *
     * @param connection 数据库连接 / Database connection
     * Schema resource path
     *
     * @param connection
     * @throws SQLException 执行 SQL 失败时 / When SQL execution fails
     * @param schemaResource
     * @throws IOException 读取资源失败时 / When reading the resource fails
     */
    private static void executeScript(Connection connection, String schemaResource) throws SQLException, IOException {
        for (String statementSql : splitStatements(readResource(schemaResource))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    /**
     * 读取 classpath 中的 schema 资源文本
     * Read schema resource text from the classpath
     *
     * Resource path
     * Script text
     *
     * @param schemaResource
     * @throws IOException 资源缺失或读取失败时 / When the resource is missing or unreadable
     */
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

    /**
     * 按分号拆分 SQL 脚本，忽略引号内的分号
     * Split a SQL script by semicolons while ignoring semicolons inside quotes
     *
     * Script text
     * Statement list
     */
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

    /**
     * 将非空语句追加到列表
     * Append a non-empty statement to the list
     *
     * Statement list
     * @param current 当前缓冲区 / Current buffer
     */
    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    /**
     * JDBC URL 解析结果：服务器 URL 与数据库名
     * Parsed JDBC URL target: server URL and database name
     *
     * @param serverUrl 不含库名的服务器 URL / Server URL without database name
     * Database name
     */
    record JdbcTarget(String serverUrl, String database) {

        private static final String MYSQL_PREFIX = "jdbc:mysql://";

        /**
         * 从 MySQL JDBC URL 解析服务器地址与库名
         * Parse server URL and database name from a MySQL JDBC URL
         *
         * JDBC URL
         * Parsed target
         */
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
