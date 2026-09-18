package com.aionemu.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link ConfigSnapshot} 的契约测试。
 * Contract tests for {@link ConfigSnapshot}.
 */
class ConfigSnapshotTest {

    /** 测试用配置载体 / Configuration holder used by this test. */
    static class SampleConfig {
        static int VALUE = 1;
        static String NAME = "before";
        static final int CONSTANT = 42;
    }

    @Test
    void restoresCapturedFieldsAfterMutation() {
        ConfigSnapshot snapshot = ConfigSnapshot.of(SampleConfig.class, "VALUE", "NAME");

        SampleConfig.VALUE = 99;
        SampleConfig.NAME = "after";

        snapshot.restore();

        assertEquals(1, SampleConfig.VALUE);
        assertEquals("before", SampleConfig.NAME);
    }

    @Test
    void rejectsUnknownFieldNames() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> ConfigSnapshot.of(SampleConfig.class, "MISSING"));
    }

}
