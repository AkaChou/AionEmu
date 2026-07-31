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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
	/** 创建玩家任务图状态表。 / Creates the player quest graph state table. */
	static final String PLAYER_QUEST_GRAPH_STATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `al_server_gs`.`player_quest_graph_states` ("
		+ "`player_id` INT NOT NULL, "
		+ "`quest_id` INT UNSIGNED NOT NULL, "
		+ "`definition_version` INT UNSIGNED NOT NULL, "
		+ "`revision` BIGINT UNSIGNED NOT NULL, "
		+ "`node_id` VARCHAR(128) NOT NULL, "
		+ "`lifecycle` VARCHAR(16) NOT NULL, "
		+ "`instance_run_id` BIGINT UNSIGNED NULL, "
		+ "`next_deadline_at` BIGINT UNSIGNED NULL, "
		+ "`state_payload` MEDIUMBLOB NOT NULL, "
		+ "PRIMARY KEY (`player_id`,`quest_id`), "
		+ "KEY `idx_player_quest_graph_deadline` (`next_deadline_at`), "
		+ "CONSTRAINT `player_quest_graph_states_ibfk_1` FOREIGN KEY (`player_id`) "
		+ "REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
	/** Creates the durable operation-key to quest graph resource identity registry. */
	static final String QUEST_GRAPH_RESOURCE_OPERATION_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `al_server_gs`.`quest_graph_resource_operations` ("
		+ "`player_id` INT NOT NULL, "
		+ "`operation_hash` BINARY(32) NOT NULL, "
		+ "`operation_key` VARCHAR(1024) NOT NULL, "
		+ "`quest_id` INT UNSIGNED NOT NULL, "
		+ "`capability` VARCHAR(64) NOT NULL, "
		+ "`reserved_object_id` INT UNSIGNED NULL, "
		+ "`resource_payload` MEDIUMBLOB NOT NULL, "
		+ "PRIMARY KEY (`player_id`,`operation_hash`), "
		+ "UNIQUE KEY `uq_quest_graph_resource_object` (`reserved_object_id`), "
		+ "KEY `idx_quest_graph_resource_quest` (`player_id`,`quest_id`), "
		+ "CONSTRAINT `quest_graph_resource_operations_ibfk_1` FOREIGN KEY (`player_id`) "
		+ "REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
	/** Creates the durable accepted outbox for asynchronous quest graph actions. */
	static final String QUEST_GRAPH_ACTION_OUTBOX_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `al_server_gs`.`quest_graph_action_outbox` ("
		+ "`outbox_sequence` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, "
		+ "`player_id` INT NOT NULL, "
		+ "`operation_hash` BINARY(32) NOT NULL, "
		+ "`operation_key` VARCHAR(1024) NOT NULL, "
		+ "`quest_id` INT UNSIGNED NOT NULL, "
		+ "`base_revision` BIGINT NOT NULL, "
		+ "`transition_id` VARCHAR(255) NOT NULL, "
		+ "`action_index` INT UNSIGNED NOT NULL, "
		+ "`command_payload` MEDIUMBLOB NOT NULL, "
		+ "`status` VARCHAR(16) NOT NULL, "
		+ "`claim_generation` BIGINT UNSIGNED NOT NULL DEFAULT 0, "
		+ "`lease_until` BIGINT UNSIGNED NULL, "
		+ "`accepted_at` BIGINT UNSIGNED NOT NULL, "
		+ "`completed_at` BIGINT UNSIGNED NULL, "
		+ "`graph_acked` TINYINT UNSIGNED NOT NULL DEFAULT 0, "
		+ "PRIMARY KEY (`player_id`,`operation_hash`), "
		+ "UNIQUE KEY `uq_quest_graph_action_outbox_sequence` (`outbox_sequence`), "
		+ "KEY `idx_quest_graph_action_outbox_pending` (`player_id`,`outbox_sequence`), "
		+ "KEY `idx_quest_graph_action_outbox_lease` (`status`,`lease_until`), "
		+ "KEY `idx_quest_graph_action_outbox_gc` (`status`,`graph_acked`,`completed_at`), "
		+ "CONSTRAINT `quest_graph_action_outbox_ibfk_1` FOREIGN KEY (`player_id`) "
		+ "REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
	/** Creates the durable cross-owner zone-mission signal ledger. */
	static final String QUEST_GRAPH_ZONE_MISSION_SIGNAL_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `al_server_gs`.`quest_graph_zone_mission_signals` ("
		+ "`player_id` INT NOT NULL, `event_hash` BINARY(32) NOT NULL, `event_id` VARCHAR(255) NOT NULL, "
		+ "`occurred_at` BIGINT UNSIGNED NOT NULL, `source_quest_id` INT UNSIGNED NOT NULL, "
		+ "`target_quest_id` INT UNSIGNED NOT NULL, `status` VARCHAR(16) NOT NULL, "
		+ "`claim_generation` BIGINT UNSIGNED NOT NULL, `lease_until` BIGINT UNSIGNED NOT NULL, "
		+ "PRIMARY KEY (`player_id`,`event_hash`), UNIQUE KEY `uq_quest_graph_zone_signal_event` (`event_id`), "
		+ "KEY `idx_quest_graph_zone_signal_target` (`player_id`,`target_quest_id`,`status`), "
		+ "KEY `idx_quest_graph_zone_signal_lease` (`status`,`lease_until`), "
		+ "CONSTRAINT `quest_graph_zone_mission_signals_ibfk_1` FOREIGN KEY (`player_id`) "
		+ "REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8";

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
                // TODO(compat-20260728): Remove this repair and its test after three production releases.
                repairRolledBackInstanceSchema(connection, target.database());
                migrateGodstoneProcCount(connection, target.database());
                migrateLimitedQuestCounters(connection, target.database());
				migratePlayerQuestGraphStates(connection, target.database());
				migrateQuestGraphResourceOperations(connection, target.database());
					migrateQuestGraphActionOutbox(connection, target.database());
					migrateQuestGraphZoneMissionSignals(connection, target.database());
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

    private static void repairRolledBackInstanceSchema(Connection connection, String database) throws SQLException {
        if (!"al_server_gs".equals(database)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `al_server_gs`.`portal_cooldowns` ("
                + "`player_id` int(11) NOT NULL, "
                + "`world_id` int(11) NOT NULL, "
                + "`reuse_time` bigint(13) NOT NULL, "
                + "`entry_count` int(2) NOT NULL, "
                + "PRIMARY KEY (`player_id`,`world_id`), "
                + "CONSTRAINT `portal_cooldowns_ibfk_1` FOREIGN KEY (`player_id`) "
                + "REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            statement.execute("CREATE TABLE IF NOT EXISTS `al_server_gs`.`player_luna_shop` ("
                + "`player_id` int(10) NOT NULL, "
                + "`free_under` tinyint(1) NOT NULL, "
                + "`free_munition` tinyint(1) NOT NULL, "
                + "`free_chest` tinyint(1) NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=latin1");
            for (String sql : rollbackRepairStatements(tableColumns(connection, database, "player_luna_shop"))) {
                statement.execute(sql);
            }
        }
    }

    static List<String> rollbackRepairStatements(Set<String> lunaColumns) {
        List<String> statements = new ArrayList<>();
        if (!lunaColumns.contains("free_under")) {
            statements.add("ALTER TABLE `al_server_gs`.`player_luna_shop` "
                + "ADD COLUMN `free_under` tinyint(1) NOT NULL DEFAULT 1 AFTER `player_id`");
        }
        if (!lunaColumns.contains("free_munition")) {
            statements.add("ALTER TABLE `al_server_gs`.`player_luna_shop` "
                + "ADD COLUMN `free_munition` tinyint(1) NOT NULL DEFAULT 1 AFTER `free_under`");
        }
        if (!lunaColumns.contains("free_chest")) {
            statements.add("ALTER TABLE `al_server_gs`.`player_luna_shop` "
                + "ADD COLUMN `free_chest` tinyint(1) NOT NULL DEFAULT 1 AFTER `free_munition`");
        }
        statements.add("DROP TABLE IF EXISTS `al_server_gs`.`instance_reward_ledger`");
        statements.add("DROP TABLE IF EXISTS `al_server_gs`.`dynamic_instance_members`");
        statements.add("DROP TABLE IF EXISTS `al_server_gs`.`dynamic_instances`");
        statements.add("DROP TABLE IF EXISTS `al_server_gs`.`player_instance_limits`");
        return statements;
    }

    private static Set<String> tableColumns(Connection connection, String database, String table) throws SQLException {
        String query = "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
        Set<String> columns = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString(1));
                }
            }
        }
        return columns;
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

	/**
	 * 为已有游戏数据库幂等创建玩家任务图状态表。
	 * Idempotently creates the player quest graph state table in an existing game database.
	 */
	private static void migratePlayerQuestGraphStates(Connection connection, String database) throws SQLException {
		if (!"al_server_gs".equals(database)) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(PLAYER_QUEST_GRAPH_STATE_TABLE_SQL);
		}
	}

	/** Idempotently creates the durable quest graph resource-operation registry. */
	private static void migrateQuestGraphResourceOperations(Connection connection, String database) throws SQLException {
		if (!"al_server_gs".equals(database)) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(QUEST_GRAPH_RESOURCE_OPERATION_TABLE_SQL);
		}
		validateQuestGraphResourceOperationSchema(connection, database);
	}

	/** Idempotently creates the durable cross-owner zone-mission signal ledger. */
	private static void migrateQuestGraphZoneMissionSignals(Connection connection, String database) throws SQLException {
		if (!"al_server_gs".equals(database)) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(QUEST_GRAPH_ZONE_MISSION_SIGNAL_TABLE_SQL);
		}
		validateQuestGraphZoneMissionSignalSchema(connection, database);
	}

	/** Validates the durable zone-mission signal ledger used by the graph bridge. */
	private static void validateQuestGraphZoneMissionSignalSchema(Connection connection, String database) throws SQLException {
		Map<String, SchemaColumn> columns = new HashMap<>();
		String columnsQuery = "SELECT column_name, data_type, column_type, is_nullable, character_maximum_length "
			+ "FROM information_schema.columns WHERE table_schema = ? AND table_name = 'quest_graph_zone_mission_signals'";
		try (PreparedStatement statement = connection.prepareStatement(columnsQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					long maximumLength = resultSet.getLong("character_maximum_length");
					Long nullableMaximumLength = resultSet.wasNull() ? null : maximumLength;
					columns.put(resultSet.getString("column_name"), new SchemaColumn(resultSet.getString("data_type"),
						resultSet.getString("column_type"), "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
						nullableMaximumLength));
				}
			}
		}

		List<SchemaIndexColumn> indexes = new ArrayList<>();
		String indexesQuery = "SELECT index_name, non_unique, seq_in_index, column_name FROM information_schema.statistics "
			+ "WHERE table_schema = ? AND table_name = 'quest_graph_zone_mission_signals'";
		try (PreparedStatement statement = connection.prepareStatement(indexesQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					indexes.add(new SchemaIndexColumn(resultSet.getString("index_name"), resultSet.getInt("non_unique") == 0,
						resultSet.getInt("seq_in_index"), resultSet.getString("column_name")));
				}
			}
		}

		List<String> violations = questGraphZoneMissionSignalSchemaViolations(columns, indexes,
			hasQuestGraphZoneMissionSignalPlayerCascadeForeignKey(connection, database));
		if (!violations.isEmpty()) {
			throw new SQLException("Existing quest_graph_zone_mission_signals schema violates the runtime contract: "
				+ String.join(", ", violations));
		}
	}

	static List<String> questGraphZoneMissionSignalSchemaViolations(Map<String, SchemaColumn> columns,
			List<SchemaIndexColumn> indexes) {
		return questGraphZoneMissionSignalSchemaViolations(columns, indexes, true);
	}

	static List<String> questGraphZoneMissionSignalSchemaViolations(Map<String, SchemaColumn> columns,
			List<SchemaIndexColumn> indexes, boolean hasPlayerCascadeForeignKey) {
		List<String> violations = new ArrayList<>();
		requireColumn(violations, columns, "player_id", "int", false, null, false);
		requireColumn(violations, columns, "event_hash", "binary", false, 32L, false);
		requireColumn(violations, columns, "event_id", "varchar", false, 255L, false);
		requireColumn(violations, columns, "occurred_at", "bigint", false, null, true);
		requireColumn(violations, columns, "source_quest_id", "int", false, null, true);
		requireColumn(violations, columns, "target_quest_id", "int", false, null, true);
		requireColumn(violations, columns, "status", "varchar", false, 16L, false);
		requireColumn(violations, columns, "claim_generation", "bigint", false, null, true);
		requireColumn(violations, columns, "lease_until", "bigint", false, null, true);
		if (!hasExactIndex(indexes, true, List.of("player_id", "event_hash"))) {
			violations.add("missing primary/unique player_id+event_hash index");
		}
		if (!hasExactIndex(indexes, true, List.of("event_id"))) {
			violations.add("missing unique event_id index");
		}
		if (!hasExactIndex(indexes, false, List.of("player_id", "target_quest_id", "status"))) {
			violations.add("missing player target/status lookup index");
		}
		if (!hasExactIndex(indexes, false, List.of("status", "lease_until"))) {
			violations.add("missing expired lease lookup index");
		}
		if (!hasPlayerCascadeForeignKey) {
			violations.add("missing cascading player_id foreign key");
		}
		return List.copyOf(violations);
	}

	private static void validateQuestGraphResourceOperationSchema(Connection connection, String database) throws SQLException {
		Map<String, SchemaColumn> columns = new HashMap<>();
		String columnsQuery = "SELECT column_name, data_type, column_type, is_nullable, character_maximum_length "
			+ "FROM information_schema.columns WHERE table_schema = ? AND table_name = 'quest_graph_resource_operations'";
		try (PreparedStatement statement = connection.prepareStatement(columnsQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					long maximumLength = resultSet.getLong("character_maximum_length");
					Long nullableMaximumLength = resultSet.wasNull() ? null : maximumLength;
					columns.put(resultSet.getString("column_name"), new SchemaColumn(resultSet.getString("data_type"),
						resultSet.getString("column_type"), "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
						nullableMaximumLength));
				}
			}
		}

		List<SchemaIndexColumn> indexes = new ArrayList<>();
		String indexesQuery = "SELECT index_name, non_unique, seq_in_index, column_name FROM information_schema.statistics "
			+ "WHERE table_schema = ? AND table_name = 'quest_graph_resource_operations'";
		try (PreparedStatement statement = connection.prepareStatement(indexesQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					indexes.add(new SchemaIndexColumn(resultSet.getString("index_name"), resultSet.getInt("non_unique") == 0,
						resultSet.getInt("seq_in_index"), resultSet.getString("column_name")));
				}
			}
		}

		List<String> violations = questGraphResourceOperationSchemaViolations(columns, indexes);
		if (!violations.isEmpty()) {
			throw new SQLException("Existing quest_graph_resource_operations schema violates the runtime contract: "
				+ String.join(", ", violations));
		}
	}

	static List<String> questGraphResourceOperationSchemaViolations(Map<String, SchemaColumn> columns,
			List<SchemaIndexColumn> indexes) {
		List<String> violations = new ArrayList<>();
		requireColumn(violations, columns, "player_id", "int", false, null, false);
		requireColumn(violations, columns, "operation_hash", "binary", false, 32L, false);
		requireColumn(violations, columns, "operation_key", "varchar", false, 1024L, false);
		requireColumn(violations, columns, "quest_id", "int", false, null, true);
		requireColumn(violations, columns, "capability", "varchar", false, 64L, false);
		requireColumn(violations, columns, "reserved_object_id", "int", true, null, true);
		requireColumn(violations, columns, "resource_payload", "mediumblob", false, null, false);
		if (!hasExactIndex(indexes, true, List.of("player_id", "operation_hash"))) {
			violations.add("missing primary/unique player_id+operation_hash index");
		}
		if (!hasExactIndex(indexes, true, List.of("reserved_object_id"))) {
			violations.add("missing unique reserved_object_id index");
		}
		if (!hasExactIndex(indexes, false, List.of("player_id", "quest_id"))) {
			violations.add("missing player_id+quest_id lookup index");
		}
		return List.copyOf(violations);
	}

	/** Idempotently creates and validates the durable quest graph action outbox. */
	private static void migrateQuestGraphActionOutbox(Connection connection, String database) throws SQLException {
		if (!"al_server_gs".equals(database)) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(QUEST_GRAPH_ACTION_OUTBOX_TABLE_SQL);
		}
		validateQuestGraphActionOutboxSchema(connection, database);
	}

	private static void validateQuestGraphActionOutboxSchema(Connection connection, String database) throws SQLException {
		Map<String, SchemaColumn> columns = new HashMap<>();
		String columnsQuery = "SELECT column_name, data_type, column_type, is_nullable, character_maximum_length, extra "
			+ "FROM information_schema.columns WHERE table_schema = ? AND table_name = 'quest_graph_action_outbox'";
		try (PreparedStatement statement = connection.prepareStatement(columnsQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					long maximumLength = resultSet.getLong("character_maximum_length");
					Long nullableMaximumLength = resultSet.wasNull() ? null : maximumLength;
					columns.put(resultSet.getString("column_name"), new SchemaColumn(resultSet.getString("data_type"),
							resultSet.getString("column_type"), "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
							nullableMaximumLength, resultSet.getString("extra")));
				}
			}
		}

		List<SchemaIndexColumn> indexes = new ArrayList<>();
		String indexesQuery = "SELECT index_name, non_unique, seq_in_index, column_name FROM information_schema.statistics "
			+ "WHERE table_schema = ? AND table_name = 'quest_graph_action_outbox'";
		try (PreparedStatement statement = connection.prepareStatement(indexesQuery)) {
			statement.setString(1, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					indexes.add(new SchemaIndexColumn(resultSet.getString("index_name"), resultSet.getInt("non_unique") == 0,
						resultSet.getInt("seq_in_index"), resultSet.getString("column_name")));
				}
			}
		}

		List<String> violations = questGraphActionOutboxSchemaViolations(columns, indexes,
			hasQuestGraphActionOutboxPlayerCascadeForeignKey(connection, database));
		if (!violations.isEmpty()) {
			throw new SQLException("Existing quest_graph_action_outbox schema violates the runtime contract: "
				+ String.join(", ", violations));
		}
	}

	static List<String> questGraphActionOutboxSchemaViolations(Map<String, SchemaColumn> columns,
			List<SchemaIndexColumn> indexes) {
		return questGraphActionOutboxSchemaViolations(columns, indexes, true);
	}

	static List<String> questGraphActionOutboxSchemaViolations(Map<String, SchemaColumn> columns,
			List<SchemaIndexColumn> indexes, boolean hasPlayerCascadeForeignKey) {
		List<String> violations = new ArrayList<>();
		requireColumn(violations, columns, "outbox_sequence", "bigint", false, null, true);
		SchemaColumn sequence = columns.get("outbox_sequence");
		if (sequence == null || !sequence.extra().toLowerCase(Locale.ROOT).contains("auto_increment")) {
			violations.add("invalid outbox_sequence auto increment");
		}
		requireColumn(violations, columns, "player_id", "int", false, null, false);
		requireColumn(violations, columns, "operation_hash", "binary", false, 32L, false);
		requireColumn(violations, columns, "operation_key", "varchar", false, 1024L, false);
		requireColumn(violations, columns, "quest_id", "int", false, null, true);
		requireColumn(violations, columns, "base_revision", "bigint", false, null, false);
		requireColumn(violations, columns, "transition_id", "varchar", false, 255L, false);
		requireColumn(violations, columns, "action_index", "int", false, null, true);
		requireColumn(violations, columns, "command_payload", "mediumblob", false, null, false);
		requireColumn(violations, columns, "status", "varchar", false, 16L, false);
		requireColumn(violations, columns, "claim_generation", "bigint", false, null, true);
		requireColumn(violations, columns, "lease_until", "bigint", true, null, true);
		requireColumn(violations, columns, "accepted_at", "bigint", false, null, true);
		requireColumn(violations, columns, "completed_at", "bigint", true, null, true);
		requireColumn(violations, columns, "graph_acked", "tinyint", false, null, true);
		if (!hasExactIndex(indexes, true, List.of("player_id", "operation_hash"))) {
			violations.add("missing primary/unique player_id+operation_hash index");
		}
		if (!hasExactIndex(indexes, true, List.of("outbox_sequence"))) {
			violations.add("missing unique outbox_sequence index");
		}
		if (!hasExactIndex(indexes, false, List.of("player_id", "outbox_sequence"))) {
			violations.add("missing player pending lookup index");
		}
		if (!hasExactIndex(indexes, false, List.of("status", "lease_until"))) {
			violations.add("missing expired lease lookup index");
		}
		if (!hasExactIndex(indexes, false, List.of("status", "graph_acked", "completed_at"))) {
			violations.add("missing acknowledged completion gc index");
		}
		if (!hasPlayerCascadeForeignKey) {
			violations.add("missing cascading player_id foreign key");
		}
		return List.copyOf(violations);
	}

	private static boolean hasQuestGraphActionOutboxPlayerCascadeForeignKey(Connection connection, String database)
			throws SQLException {
		String query = "SELECT COUNT(*) FROM information_schema.key_column_usage kcu "
			+ "JOIN information_schema.referential_constraints rc ON rc.constraint_schema = kcu.constraint_schema "
			+ "AND rc.constraint_name = kcu.constraint_name AND rc.table_name = kcu.table_name "
			+ "WHERE kcu.table_schema = ? AND kcu.table_name = 'quest_graph_action_outbox' "
			+ "AND kcu.column_name = 'player_id' AND kcu.referenced_table_schema = ? "
			+ "AND kcu.referenced_table_name = 'players' AND kcu.referenced_column_name = 'id' "
			+ "AND rc.delete_rule = 'CASCADE' AND rc.update_rule = 'CASCADE'";
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, database);
			statement.setString(2, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getInt(1) == 1;
			}
		}
	}

	private static boolean hasQuestGraphZoneMissionSignalPlayerCascadeForeignKey(Connection connection, String database)
			throws SQLException {
		String query = "SELECT COUNT(*) FROM information_schema.key_column_usage kcu "
			+ "JOIN information_schema.referential_constraints rc ON rc.constraint_schema = kcu.constraint_schema "
			+ "AND rc.constraint_name = kcu.constraint_name AND rc.table_name = kcu.table_name "
			+ "WHERE kcu.table_schema = ? AND kcu.table_name = 'quest_graph_zone_mission_signals' "
			+ "AND kcu.column_name = 'player_id' AND kcu.referenced_table_schema = ? "
			+ "AND kcu.referenced_table_name = 'players' AND kcu.referenced_column_name = 'id' "
			+ "AND rc.delete_rule = 'CASCADE' AND rc.update_rule = 'CASCADE'";
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, database);
			statement.setString(2, database);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getInt(1) == 1;
			}
		}
	}

	private static void requireColumn(List<String> violations, Map<String, SchemaColumn> columns, String name,
			String dataType, boolean nullable, Long maximumLength, boolean unsigned) {
		SchemaColumn actual = columns.get(name);
		if (actual == null || !dataType.equalsIgnoreCase(actual.dataType()) || actual.nullable() != nullable
				|| maximumLength != null && !maximumLength.equals(actual.maximumLength())
				|| unsigned != actual.columnType().toLowerCase(Locale.ROOT).contains("unsigned")) {
			violations.add("invalid " + name + " column");
		}
	}

	private static boolean hasExactIndex(List<SchemaIndexColumn> indexes, boolean unique, List<String> columns) {
		Map<String, List<SchemaIndexColumn>> byName = new HashMap<>();
		for (SchemaIndexColumn index : indexes) {
			byName.computeIfAbsent(index.name(), ignored -> new ArrayList<>()).add(index);
		}
		return byName.values().stream().anyMatch(index -> {
			index.sort(Comparator.comparingInt(SchemaIndexColumn::sequence));
			return index.size() == columns.size() && index.stream().allMatch(column -> column.unique() == unique)
				&& index.stream().map(SchemaIndexColumn::column).toList().equals(columns);
		});
	}

	record SchemaColumn(String dataType, String columnType, boolean nullable, Long maximumLength, String extra) {
		SchemaColumn(String dataType, String columnType, boolean nullable, Long maximumLength) {
			this(dataType, columnType, nullable, maximumLength, "");
		}

		SchemaColumn {
			extra = extra == null ? "" : extra;
		}
	}

	record SchemaIndexColumn(String name, boolean unique, int sequence, String column) {
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
