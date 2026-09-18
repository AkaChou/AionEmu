package com.aionemu.testutil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 配置类静态字段的快照与还原工具。
 *
 * <p>大量测试会临时改写 {@code XxxConfig} 的静态字段，然后手写 getter/字段备份与 {@code @AfterEach}
 * 还原。这种样板容易漏还原、也容易在新增字段时忘记同步，从而把状态泄漏给后续测试。本工具用一次反射快照
 * 取代这段样板：</p>
 *
 * <pre>{@code
 * private final ConfigSnapshot configSnapshot = ConfigSnapshot.of(MembershipConfig.class,
 *     "STORE_WH_ALL", "TRADE_ALL");
 *
 * @AfterEach
 * void restoreConfig() {
 *     configSnapshot.restore();
 * }
 * }</pre>
 *
 * <p>{@link #of(Class, String...)} 要求目标字段存在且为 {@code static}，拼写错误会立刻失败而不是静默跳过。</p>
 *
 * Snapshot and restore helper for static configuration fields.
 *
 * <p>Many tests temporarily overwrite {@code XxxConfig} static fields and then hand-write the backup
 * plus an {@code @AfterEach} restore. That boilerplate is easy to get wrong and easy to forget when a
 * field is added, leaking state into later tests. This helper replaces it with a single reflective
 * snapshot. {@link #of(Class, String...)} requires every named field to exist and be static, so a typo
 * fails fast instead of silently doing nothing.</p>
 */
public final class ConfigSnapshot {

    /**
     * 已捕获的字段。
     * Captured fields.
     */
    private final List<Field> fields;

    /**
     * 与 {@link #fields} 一一对应的原始值。
     * Original values, aligned with {@link #fields}.
     */
    private final List<Object> values;

    /**
     * 私有构造器。
     * Private constructor.
     */
    private ConfigSnapshot(List<Field> fields, List<Object> values) {
        this.fields = fields;
        this.values = values;
    }

    /**
     * 捕获指定配置类的一组静态字段。
     * Captures the named static fields of a configuration class.
     *
     * @param configType 配置类 / configuration type
     * @param fieldNames 静态字段名 / static field names
     * @return 快照 / snapshot
     */
    public static ConfigSnapshot of(Class<?> configType, String... fieldNames) {
        Objects.requireNonNull(configType, "configType");
        Objects.requireNonNull(fieldNames, "fieldNames");
        if (fieldNames.length == 0) {
            throw new IllegalArgumentException("At least one field name is required for " + configType.getName());
        }
        List<Field> fields = new ArrayList<>(fieldNames.length);
        List<Object> values = new ArrayList<>(fieldNames.length);
        for (String fieldName : fieldNames) {
            Field field = resolveStaticField(configType, fieldName);
            try {
                fields.add(field);
                values.add(field.get(null));
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot read " + configType.getName() + "." + fieldName, failure);
            }
        }
        return new ConfigSnapshot(fields, values);
    }

    /**
     * 还原全部已捕获字段；单个字段失败不中断其余字段。
     * Restores every captured field; a failure on one field does not stop the others.
     */
    public void restore() {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            boolean oldAccessible = field.canAccess(null);
            try {
                field.setAccessible(true);
                field.set(null, values.get(i));
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot restore " + field, failure);
            } finally {
                field.setAccessible(oldAccessible);
            }
        }
    }

    /**
     * 解析并校验静态字段。
     * Resolves and validates a static field.
     */
    private static Field resolveStaticField(Class<?> configType, String fieldName) {
        try {
            Field field = configType.getDeclaredField(fieldName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException(
                    configType.getName() + "." + fieldName + " must be static to snapshot it");
            }
            return field;
        } catch (NoSuchFieldException failure) {
            throw new IllegalArgumentException(
                configType.getName() + "." + fieldName + " does not exist; known fields: "
                    + Arrays.toString(configType.getDeclaredFields()), failure);
        }
    }
}
