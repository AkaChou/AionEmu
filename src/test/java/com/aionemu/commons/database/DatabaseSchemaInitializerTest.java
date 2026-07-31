package com.aionemu.commons.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatabaseSchemaInitializerTest {

    @Test
    void parsesMySqlDatabaseNameAndServerUrl() {
        DatabaseSchemaInitializer.JdbcTarget target = DatabaseSchemaInitializer.JdbcTarget.from(
            "jdbc:mysql://127.0.0.1:3306/al_server_gs?useUnicode=true&serverTimezone=UTC"
        );

        assertEquals("jdbc:mysql://127.0.0.1:3306/?useUnicode=true&serverTimezone=UTC", target.serverUrl());
        assertEquals("al_server_gs", target.database());
    }

    @Test
    void rejectsUrlsWithoutDatabaseName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DatabaseSchemaInitializer.JdbcTarget.from("jdbc:mysql://127.0.0.1:3306/?useUnicode=true")
        );
    }

    @Test
    void resolvesBundledBaselineSchemas() {
        assertEquals("db/mysql/al_server_gs.sql", DatabaseSchemaInitializer.schemaResource("al_server_gs"));
        assertEquals("db/mysql/al_server_ls.sql", DatabaseSchemaInitializer.schemaResource("al_server_ls"));
    }

    @Test
    void gameServerSchemaIncludesWebRewardTableUsedByRewardDao() throws IOException {
        String schema = resourceText("db/mysql/al_server_gs.sql");

        assertTrue(schema.contains("CREATE TABLE `web_reward`"));
        assertTrue(schema.contains("`unique` int(11) NOT NULL AUTO_INCREMENT"));
        assertTrue(schema.contains("`item_owner` int(11) NOT NULL"));
        assertTrue(schema.contains("`item_id` int(11) NOT NULL"));
        assertTrue(schema.contains("`item_count` bigint(20) NOT NULL DEFAULT '0'"));
        assertTrue(schema.contains("`rewarded` tinyint(1) NOT NULL DEFAULT '0'"));
        assertTrue(schema.contains("`received` timestamp NULL DEFAULT NULL"));
    }

    @Test
    void gameServerSchemaIncludesPersistentLimitedQuestCounters() throws IOException {
        String schema = resourceText("db/mysql/al_server_gs.sql");

        assertTrue(schema.contains("CREATE TABLE `limited_quest_counters`"));
        assertTrue(schema.contains("`quest_id` int(10) unsigned NOT NULL"));
        assertTrue(schema.contains("`remaining` int(10) unsigned NOT NULL"));
        assertTrue(schema.contains("PRIMARY KEY (`quest_id`)"));
    }

	@Test
	void gameServerSchemaAndExistingDatabaseMigrationIncludeQuestGraphState() throws IOException {
		String schema = resourceText("db/mysql/al_server_gs.sql");

		assertTrue(schema.contains("CREATE TABLE `player_quest_graph_states`"));
		assertTrue(schema.contains("`definition_version` int(10) unsigned NOT NULL"));
		assertTrue(schema.contains("`next_deadline_at` bigint(20) unsigned DEFAULT NULL"));
		assertTrue(schema.contains("`state_payload` mediumblob NOT NULL"));
		assertTrue(schema.contains("KEY `idx_player_quest_graph_deadline` (`next_deadline_at`)"));
		assertTrue(DatabaseSchemaInitializer.PLAYER_QUEST_GRAPH_STATE_TABLE_SQL.contains("CREATE TABLE IF NOT EXISTS"));
		assertTrue(DatabaseSchemaInitializer.PLAYER_QUEST_GRAPH_STATE_TABLE_SQL.contains("FOREIGN KEY (`player_id`)"));
		assertTrue(schema.contains("CREATE TABLE `quest_graph_resource_operations`"));
		assertTrue(schema.contains("UNIQUE KEY `uq_quest_graph_resource_object` (`reserved_object_id`)"));
		assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_RESOURCE_OPERATION_TABLE_SQL.contains("`operation_hash` BINARY(32)"));
		assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_RESOURCE_OPERATION_TABLE_SQL.contains("`resource_payload` MEDIUMBLOB"));
		assertTrue(schema.contains("CREATE TABLE `quest_graph_action_outbox`"));
			assertTrue(schema.contains("`outbox_sequence` bigint(20) unsigned NOT NULL AUTO_INCREMENT"));
			assertTrue(schema.contains("`base_revision` bigint(20) NOT NULL"));
		assertTrue(schema.contains("`claim_generation` bigint(20) unsigned NOT NULL DEFAULT '0'"));
		assertTrue(schema.contains("KEY `idx_quest_graph_action_outbox_lease` (`status`,`lease_until`)"));
			assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_ACTION_OUTBOX_TABLE_SQL.contains("CREATE TABLE IF NOT EXISTS"));
			assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_ACTION_OUTBOX_TABLE_SQL.contains("`outbox_sequence` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT"));
		assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_ACTION_OUTBOX_TABLE_SQL.contains("`base_revision` BIGINT NOT NULL"));
		assertTrue(schema.contains("CREATE TABLE `quest_graph_zone_mission_signals`"));
		assertTrue(schema.contains("`event_hash` binary(32) NOT NULL"));
		assertTrue(schema.contains("UNIQUE KEY `uq_quest_graph_zone_signal_event` (`event_id`)"));
		assertTrue(DatabaseSchemaInitializer.QUEST_GRAPH_ZONE_MISSION_SIGNAL_TABLE_SQL.contains("FOREIGN KEY (`player_id`)"));
	}

	@Test
	void questGraphResourceOperationSchemaRequiresFullColumnsAndObjectIdUniqueness() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = validResourceOperationColumns();
		List<DatabaseSchemaInitializer.SchemaIndexColumn> indexes = validResourceOperationIndexes();

		assertTrue(DatabaseSchemaInitializer.questGraphResourceOperationSchemaViolations(columns, indexes).isEmpty());

		columns.put("operation_key", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(512)", false, 512L));
		List<DatabaseSchemaInitializer.SchemaIndexColumn> nonUniqueObjectId = new ArrayList<>(indexes);
		nonUniqueObjectId.set(2,
			new DatabaseSchemaInitializer.SchemaIndexColumn("uq_quest_graph_resource_object", false, 1, "reserved_object_id"));
		List<String> violations = DatabaseSchemaInitializer.questGraphResourceOperationSchemaViolations(columns,
			nonUniqueObjectId);

		assertTrue(violations.contains("invalid operation_key column"));
		assertTrue(violations.contains("missing unique reserved_object_id index"));
	}

	@Test
	void questGraphActionOutboxSchemaRequiresSignedBaseRevisionAndExactRecoveryIndexes() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = validActionOutboxColumns();
		List<DatabaseSchemaInitializer.SchemaIndexColumn> indexes = validActionOutboxIndexes();

		assertTrue(DatabaseSchemaInitializer.questGraphActionOutboxSchemaViolations(columns, indexes).isEmpty());

		columns.put("base_revision",
			new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		List<DatabaseSchemaInitializer.SchemaIndexColumn> incompleteIndexes = new ArrayList<>(indexes);
		incompleteIndexes.remove(incompleteIndexes.size() - 1);
		List<String> violations = DatabaseSchemaInitializer.questGraphActionOutboxSchemaViolations(columns,
			incompleteIndexes);

		assertTrue(violations.contains("invalid base_revision column"));
		assertTrue(violations.contains("missing acknowledged completion gc index"));
		Map<String, DatabaseSchemaInitializer.SchemaColumn> missingSequence = validActionOutboxColumns();
		missingSequence.remove("outbox_sequence");
		assertTrue(DatabaseSchemaInitializer.questGraphActionOutboxSchemaViolations(missingSequence,
			validActionOutboxIndexes()).contains("invalid outbox_sequence auto increment"));
		assertTrue(DatabaseSchemaInitializer.questGraphActionOutboxSchemaViolations(validActionOutboxColumns(),
			validActionOutboxIndexes(), false).contains("missing cascading player_id foreign key"));
	}

	@Test
	void questGraphZoneMissionSignalSchemaRequiresLedgerColumnsIndexesAndPlayerCascade() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = validZoneMissionSignalColumns();
		List<DatabaseSchemaInitializer.SchemaIndexColumn> indexes = validZoneMissionSignalIndexes();

		assertTrue(DatabaseSchemaInitializer.questGraphZoneMissionSignalSchemaViolations(columns, indexes).isEmpty());

		columns.put("event_id", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(128)", false, 128L));
		List<DatabaseSchemaInitializer.SchemaIndexColumn> incompleteIndexes = new ArrayList<>(indexes);
		incompleteIndexes.removeIf(index -> "idx_quest_graph_zone_signal_target".equals(index.name()));
		List<String> violations = DatabaseSchemaInitializer.questGraphZoneMissionSignalSchemaViolations(columns,
			incompleteIndexes, false);

		assertTrue(violations.contains("invalid event_id column"));
		assertTrue(violations.contains("missing player target/status lookup index"));
		assertTrue(violations.contains("missing cascading player_id foreign key"));
	}

    @Test
    void repairsRolledBackLunaColumnsBeforeDroppingObsoleteInstanceTables() {
        List<String> statements = DatabaseSchemaInitializer.rollbackRepairStatements(Set.of("player_id", "free_chest"));

        assertEquals(6, statements.size());
        assertTrue(statements.get(0).contains("ADD COLUMN `free_under`"));
        assertTrue(statements.get(1).contains("ADD COLUMN `free_munition`"));
        assertTrue(statements.get(2).endsWith("`instance_reward_ledger`"));
        assertTrue(statements.get(3).endsWith("`dynamic_instance_members`"));
        assertTrue(statements.get(4).endsWith("`dynamic_instances`"));
        assertTrue(statements.get(5).endsWith("`player_instance_limits`"));
        assertEquals(statements.subList(2, 6), DatabaseSchemaInitializer.rollbackRepairStatements(
            Set.of("player_id", "free_under", "free_munition", "free_chest")));
    }

    @Test
    void splitsSqlStatementsWithoutSplittingQuotedSemicolons() {
        List<String> statements = DatabaseSchemaInitializer.splitStatements(
            "CREATE DATABASE `al_server_gs`;\n"
                + "INSERT INTO `messages` VALUES ('keep;semicolon', \"double;semicolon\");\n"
                + "USE `al_server_gs`;"
        );

        assertEquals(3, statements.size());
        assertEquals("CREATE DATABASE `al_server_gs`", statements.get(0));
        assertEquals("INSERT INTO `messages` VALUES ('keep;semicolon', \"double;semicolon\")", statements.get(1));
        assertEquals("USE `al_server_gs`", statements.get(2));
    }

    private static String resourceText(String name) throws IOException {
        try (var input = DatabaseSchemaInitializerTest.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

	private static Map<String, DatabaseSchemaInitializer.SchemaColumn> validResourceOperationColumns() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = new HashMap<>();
		columns.put("player_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(11)", false, null));
		columns.put("operation_hash", new DatabaseSchemaInitializer.SchemaColumn("binary", "binary(32)", false, 32L));
		columns.put("operation_key", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(1024)", false, 1024L));
		columns.put("quest_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", false, null));
		columns.put("capability", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(64)", false, 64L));
		columns.put("reserved_object_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", true, null));
		columns.put("resource_payload", new DatabaseSchemaInitializer.SchemaColumn("mediumblob", "mediumblob", false, null));
		return columns;
	}

	private static List<DatabaseSchemaInitializer.SchemaIndexColumn> validResourceOperationIndexes() {
		return List.of(
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 2, "operation_hash"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("uq_quest_graph_resource_object", true, 1, "reserved_object_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_resource_quest", false, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_resource_quest", false, 2, "quest_id"));
	}

	private static Map<String, DatabaseSchemaInitializer.SchemaColumn> validActionOutboxColumns() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = new HashMap<>();
		columns.put("outbox_sequence", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null,
			"auto_increment"));
		columns.put("player_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(11)", false, null));
		columns.put("operation_hash", new DatabaseSchemaInitializer.SchemaColumn("binary", "binary(32)", false, 32L));
		columns.put("operation_key", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(1024)", false, 1024L));
		columns.put("quest_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", false, null));
		columns.put("base_revision", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20)", false, null));
		columns.put("transition_id", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(255)", false, 255L));
		columns.put("action_index", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", false, null));
		columns.put("command_payload", new DatabaseSchemaInitializer.SchemaColumn("mediumblob", "mediumblob", false, null));
		columns.put("status", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(16)", false, 16L));
		columns.put("claim_generation", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		columns.put("lease_until", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", true, null));
		columns.put("accepted_at", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		columns.put("completed_at", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", true, null));
		columns.put("graph_acked", new DatabaseSchemaInitializer.SchemaColumn("tinyint", "tinyint(3) unsigned", false, null));
		return columns;
	}

	private static List<DatabaseSchemaInitializer.SchemaIndexColumn> validActionOutboxIndexes() {
		return List.of(
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 2, "operation_hash"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("uq_quest_graph_action_outbox_sequence", true, 1, "outbox_sequence"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_pending", false, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_pending", false, 2, "outbox_sequence"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_lease", false, 1, "status"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_lease", false, 2, "lease_until"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_gc", false, 1, "status"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_gc", false, 2, "graph_acked"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_action_outbox_gc", false, 3, "completed_at"));
	}

	private static Map<String, DatabaseSchemaInitializer.SchemaColumn> validZoneMissionSignalColumns() {
		Map<String, DatabaseSchemaInitializer.SchemaColumn> columns = new HashMap<>();
		columns.put("player_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(11)", false, null));
		columns.put("event_hash", new DatabaseSchemaInitializer.SchemaColumn("binary", "binary(32)", false, 32L));
		columns.put("event_id", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(255)", false, 255L));
		columns.put("occurred_at", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		columns.put("source_quest_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", false, null));
		columns.put("target_quest_id", new DatabaseSchemaInitializer.SchemaColumn("int", "int(10) unsigned", false, null));
		columns.put("status", new DatabaseSchemaInitializer.SchemaColumn("varchar", "varchar(16)", false, 16L));
		columns.put("claim_generation", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		columns.put("lease_until", new DatabaseSchemaInitializer.SchemaColumn("bigint", "bigint(20) unsigned", false, null));
		return columns;
	}

	private static List<DatabaseSchemaInitializer.SchemaIndexColumn> validZoneMissionSignalIndexes() {
		return List.of(
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("PRIMARY", true, 2, "event_hash"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("uq_quest_graph_zone_signal_event", true, 1, "event_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_zone_signal_target", false, 1, "player_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_zone_signal_target", false, 2, "target_quest_id"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_zone_signal_target", false, 3, "status"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_zone_signal_lease", false, 1, "status"),
			new DatabaseSchemaInitializer.SchemaIndexColumn("idx_quest_graph_zone_signal_lease", false, 2, "lease_until"));
	}
}
