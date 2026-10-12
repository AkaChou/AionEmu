package com.aionemu.loginserver.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

/**
 * VIP 同步 SQL 契约：INSERT IGNORE 语义保证永不覆盖已有 VIP 行。
 * Contract tests for VIP sync SQL: INSERT IGNORE must never overwrite existing VIP rows.
 */
class VipDaoSqlTest {

    @Test
    void synchronizationAndInsertionNeverOverwriteExistingVipData() throws Exception {
        String sync = insertSql("syncMissingAccounts", int.class);
        String insert = insertSql("insertIfAbsent", int.class, int.class);
        assertTrue(sync.startsWith("INSERT IGNORE"), sync);
        assertTrue(insert.startsWith("INSERT IGNORE"), insert);
        assertFalse(sync.contains("UPDATE"), sync);
        assertFalse(insert.contains("UPDATE"), insert);
    }

    private static String insertSql(String method, Class<?>... parameterTypes) throws Exception {
        Method m = AccountVipMapper.class.getMethod(method, parameterTypes);
        return m.getAnnotation(Insert.class).value()[0];
    }
}
