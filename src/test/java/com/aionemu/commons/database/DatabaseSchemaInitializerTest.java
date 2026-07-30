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
}
