package com.aionemu.commons.database;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.configs.DatabaseConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
                migrateRetailInstances(connection, target.database());
                migrateGodstoneProcCount(connection, target.database());
                migrateLimitedQuestCounters(connection, target.database());
                migrateAccountVip(connection, target.database());
                log.debug("Database {} already contains tables; skipping schema initialization.", target.database());
                return;
            }

            log.info(I18n.get("log.fe7ca44cd941", target.database(), schemaResource));
            executeScript(connection, schemaResource);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize database schema for " + target.database(), e);
        }
    }

    private static void migrateRetailInstances(Connection connection, String database) throws SQLException, IOException {
        if (!"al_server_gs".equals(database)) {
            return;
        }
        executeScript(connection, "db/mysql/retail_instance_schema.sql");
        ensureRetailMatchAdmissionColumns(connection, database);
        dropLegacyInstanceRewardStatus(connection, database);
        if (!hasTable(connection, database, "portal_cooldowns")) {
            return;
        }
        Map<Integer, Integer> syncByWorld = loadInstanceSyncKeys();
        String select = "SELECT player_id, world_id, reuse_time, entry_count FROM `al_server_gs`.`portal_cooldowns`";
        String insert = "INSERT INTO `al_server_gs`.`player_instance_limits` "
            + "(player_id, limit_key, reset_at, used, bonus_available, purchased_count, purchase_step, updated_at) "
            + "VALUES (?,?,?,?,0,0,0,?) ON DUPLICATE KEY UPDATE "
            + "reset_at=GREATEST(reset_at,VALUES(reset_at)), used=GREATEST(used,VALUES(used)), updated_at=VALUES(updated_at)";
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement query = connection.createStatement();
                ResultSet rows = query.executeQuery(select);
                PreparedStatement save = connection.prepareStatement(insert)) {
            long now = System.currentTimeMillis();
            while (rows.next()) {
                int worldId = rows.getInt("world_id");
                Integer syncId = syncByWorld.get(worldId);
                if (syncId == null) {
                    throw new SQLException("Cannot migrate portal_cooldowns world_id " + worldId + " to a retail sync key");
                }
                save.setInt(1, rows.getInt("player_id"));
                save.setInt(2, syncId);
                save.setLong(3, rows.getLong("reuse_time"));
                save.setInt(4, Math.max(0, rows.getInt("entry_count")));
                save.setLong(5, now);
                save.addBatch();
            }
            save.executeBatch();
            try (Statement drop = connection.createStatement()) {
                drop.execute("DROP TABLE `al_server_gs`.`portal_cooldowns`");
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            if (e instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new IOException("Failed to migrate retail instance limits", e);
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static void ensureRetailMatchAdmissionColumns(Connection connection, String database) throws SQLException {
        for (String definition : List.of(
            "entry_limit_key int(11) NOT NULL DEFAULT 0",
            "entry_consumed tinyint(1) NOT NULL DEFAULT 0"
        )) {
            String column = definition.substring(0, definition.indexOf(' '));
            if (!hasColumn(connection, database, "dynamic_instance_members", column)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE `al_server_gs`.`dynamic_instance_members` ADD COLUMN `" + column
                        + "` " + definition.substring(column.length() + 1));
                }
            }
        }
    }

    private static void dropLegacyInstanceRewardStatus(Connection connection, String database) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? "
            + "AND table_name='dynamic_instance_members' AND column_name='reward_status'";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                if (row.getInt(1) == 0) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `al_server_gs`.`dynamic_instance_members` DROP COLUMN `reward_status`");
        }
    }

    private static boolean hasColumn(Connection connection, String database, String table, String column)
            throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? "
            + "AND table_name=? AND column_name=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getInt(1) != 0;
            }
        }
    }

    static Map<Integer, Integer> loadInstanceSyncKeys() throws IOException {
        String resource = "aion/definitions/compact/instance/limits.xml";
        String definitionsDir = System.getProperty("aion.game.definitions.dir");
        InputStream source = definitionsDir == null
            ? DatabaseSchemaInitializer.class.getClassLoader().getResourceAsStream(resource)
            : Files.newInputStream(Path.of(definitionsDir).resolve("compact/instance/limits.xml"));
        try (InputStream input = source) {
            if (input == null) {
                throw new IOException("Missing " + resource);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            NodeList rules = factory.newDocumentBuilder().parse(input).getElementsByTagName("instance_rule");
            Map<Integer, Integer> result = new HashMap<>();
            for (int i = 0; i < rules.getLength(); i++) {
                Element rule = (Element) rules.item(i);
                String syncId = rule.getAttribute("coolt_sync_id");
                if (!syncId.isEmpty()) {
                    result.put(Integer.parseInt(rule.getAttribute("world_id")), Integer.parseInt(syncId));
                }
            }
            return result;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to read retail instance sync keys", e);
        }
    }

    private static boolean hasTable(Connection connection, String database, String table) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    private static void migrateGodstoneProcCount(Connection connection, String database) throws SQLException {
        if (!"al_server_gs".equals(database)) {
            return;
        }
        String query = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? "
            + "AND table_name = 'item_stones' AND column_name = 'proc_count'";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `al_server_gs`.`item_stones` ADD COLUMN `proc_count` INT NOT NULL DEFAULT 0 "
                + "AFTER `polishCharge`");
            log.info(I18n.get("log.59a545534c80"));
        }
    }

    private static void migrateLimitedQuestCounters(Connection connection, String database) throws SQLException {
        if (!"al_server_gs".equals(database)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `al_server_gs`.`limited_quest_counters` ("
                + "`quest_id` INT UNSIGNED NOT NULL, "
                + "`remaining` INT UNSIGNED NOT NULL, "
                + "PRIMARY KEY (`quest_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        }
    }

    private static void migrateAccountVip(Connection connection, String database) throws SQLException {
        if (!"al_server_ls".equals(database)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `al_server_ls`.`account_vip` ("
                + "`account_id` INT NOT NULL, "
                + "`vip_level` TINYINT UNSIGNED NOT NULL COMMENT 'Client VIP stage (1-6)', "
                + "`vip_exp` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'VIP progress experience', "
                + "`expire_time` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Unix seconds VIP end time', "
                + "PRIMARY KEY (`account_id`), "
                + "CONSTRAINT `FK_account_vip_account` FOREIGN KEY (`account_id`) "
                + "REFERENCES `al_server_ls`.`account_data` (`id`) ON DELETE CASCADE, "
                + "CONSTRAINT `CHK_account_vip_level` CHECK (`vip_level` BETWEEN 1 AND 6)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
        }
        String query = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? "
            + "AND table_name = 'account_vip' AND column_name = 'expire_time'";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `al_server_ls`.`account_vip` "
                + "ADD COLUMN `expire_time` BIGINT UNSIGNED NOT NULL DEFAULT 0 "
                + "COMMENT 'Unix seconds VIP end time' AFTER `vip_exp`");
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
