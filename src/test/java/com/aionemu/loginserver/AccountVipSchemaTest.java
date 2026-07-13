package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AccountVipSchemaTest {

    @Test
    void loginBaselineContainsIndependentAccountVipTable() throws IOException {
        String schema;
        try (var input = getClass().getClassLoader().getResourceAsStream("db/mysql/al_server_ls.sql")) {
            if (input == null) {
                throw new IOException("Missing login baseline schema");
            }
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(schema.contains("CREATE TABLE `account_vip`"));
        assertTrue(schema.contains("`vip_level` tinyint(3) unsigned NOT NULL COMMENT 'Client VIP stage (1-6)'"));
        assertTrue(schema.contains("`vip_exp` bigint(20) unsigned NOT NULL DEFAULT '0'"));
        assertTrue(schema.contains("PRIMARY KEY (`account_id`)"));
        assertTrue(schema.contains("FOREIGN KEY (`account_id`) REFERENCES `account_data` (`id`) ON DELETE CASCADE"));
        assertTrue(schema.contains("CHECK (`vip_level` BETWEEN 1 AND 6)"));
    }
}
