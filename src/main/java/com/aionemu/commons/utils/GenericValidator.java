package com.aionemu.commons.utils;

import java.util.Collection;
import java.util.Map;

/**
 * 通用验证工具类，用于检查各种类型的对象是否为空
 * Generic validation utility class for checking if various types of objects are empty or null
 */
public class GenericValidator {

    /**
     * 检查字符串是否为空或null
     * Check if string is empty or null
     * 
     * @param s 要检查的字符串 / String to check
     * @return 如果字符串为空或null则返回true / Returns true if string is empty or null
     */
    public static boolean isBlankOrNull(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * 检查集合是否为空或null
     * Check if collection is empty or null
     * 
     * @param c 要检查的集合 / Collection to check
     * @return 如果集合为空或null则返回true / Returns true if collection is empty or null
     */
    public static boolean isBlankOrNull(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * 检查Map是否为空或null
     * Check if map is empty or null
     * 
     * @param m 要检查的Map / Map to check
     * @return 如果Map为空或null则返回true / Returns true if map is empty or null
     */
    public static boolean isBlankOrNull(Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * 检查数字是否为空或0
     * Check if number is null or zero
     * 
     * @param n 要检查的数字 / Number to check
     * @return 如果数字为null或0则返回true / Returns true if number is null or zero
     */
    public static boolean isBlankOrNull(Number n) {
        return n == null || n.doubleValue() == 0.0D;
    }

    /**
     * 检查数组是否为空或null
     * Check if array is empty or null
     * 
     * @param a 要检查的数组 / Array to check
     * @return 如果数组为空或null则返回true / Returns true if array is empty or null
     */
    public static boolean isBlankOrNull(Object[] a) {
        return a == null || a.length == 0;
    }
}
