package com.aionemu.commons.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
}
