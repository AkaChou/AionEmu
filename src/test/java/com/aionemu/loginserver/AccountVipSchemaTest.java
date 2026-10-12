package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * VIP 表结构契约：{@code V1__init.sql} 必须带出独立账号 VIP 表及其约束。
 * Contract for the account VIP table: {@code V1__init.sql} must carry the independent VIP table
 * and its constraints.
 */
class AccountVipSchemaTest {

    @Test
    void loginBaselineContainsIndependentAccountVipTable() throws IOException {
        String schema;
        try (var input = getClass().getClassLoader().getResourceAsStream("db/migration/V1__init.sql")) {
            if (input == null) {
                throw new IOException("Missing unified schema migration");
            }
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(schema.contains("CREATE TABLE `ls_account_vip`"));
        assertTrue(schema.contains("`vip_level` tinyint unsigned NOT NULL COMMENT 'Client VIP stage (1-6)'"));
        assertTrue(schema.contains("`vip_exp` bigint unsigned NOT NULL DEFAULT '0'"));
        assertTrue(schema.contains("`expire_time` bigint unsigned NOT NULL DEFAULT '0'"));
        assertTrue(schema.contains("PRIMARY KEY (`account_id`)"));
        assertTrue(schema.contains("REFERENCES `ls_account_data` (`id`) ON DELETE CASCADE"));
        assertTrue(schema.contains("CHECK ((`vip_level` between 1 and 6))"));
    }
}
