package com.aionemu.loginserver.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

/**
 * VIP 授权 SQL 契约：批量授权必须提升已有记录（UPSERT），单账号补齐必须不覆盖已有行。
 * Contract tests for VIP grant SQL: the bulk grant must upgrade existing rows (UPSERT), the
 * single-account insert must never overwrite an existing row.
 */
class VipDaoSqlTest {

    @Test
    void bulkGrantUpgradesExistingRowsAndSingleInsertNeverOverwrites() throws Exception {
        String grant = insertSql("grantToAllAccounts", int.class, int.class);
        String insert = insertSql("insertIfAbsent", int.class, int.class, int.class);

        assertTrue(grant.startsWith("INSERT INTO"), grant);
        assertTrue(grant.contains("ON DUPLICATE KEY UPDATE"), grant);
        assertTrue(grant.contains("#{days} * 24 * 3600"), grant);

        assertTrue(insert.startsWith("INSERT IGNORE"), insert);
        assertFalse(insert.contains("UPDATE"), insert);
        assertTrue(insert.contains("#{days} * 24 * 3600"), insert);
    }

    private static String insertSql(String method, Class<?>... parameterTypes) throws Exception {
        Method m = AccountVipMapper.class.getMethod(method, parameterTypes);
        return m.getAnnotation(Insert.class).value()[0];
    }
}
