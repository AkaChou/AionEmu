package com.aionemu.commons.utils;

import java.util.Collection;
import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * 通用空值/空集合校验工具。
 * Generic blank/null validation helpers.
 */
@UtilityClass
public class GenericValidator {

    /**
     * 判断字符串是否为 null 或空。
     * Whether the string is null or empty.
     *
     * @param s 待检查字符串 / String to check
     * @return 若 blank or null 则为 true / True if blank or null
     */
    public boolean isBlankOrNull(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * 判断集合是否为 null 或空。
     * Whether the collection is null or empty.
     *
     * @param c 待检查集合 / Collection to check
     * @return 若 blank or null 则为 true / True if blank or null
     */
    public boolean isBlankOrNull(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * 判断 Map 是否为 null 或空。
     * Whether the map is null or empty.
     *
     * Map to check
     *
     * @param m 若 blank or null 则为 true / True if blank or null
     */
    public boolean isBlankOrNull(Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * 判断数字是否为 null 或 0。
     * Whether the number is null or zero.
     *
     * @param n 待检查数字 / Number to check
     * @return 为空或 0 则为 true / True if null or zero
     */
    public boolean isBlankOrNull(Number n) {
        return n == null || n.doubleValue() == 0.0D;
    }

    /**
     * 判断数组是否为 null 或长度为 0。
     * Whether the array is null or empty.
     *
     * @param a 待检查数组 / Array to check
     * @return 若 blank or null 则为 true / True if blank or null
     */
    public boolean isBlankOrNull(Object[] a) {
        return a == null || a.length == 0;
    }
}
