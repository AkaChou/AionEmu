package com.aionemu.commons.utils;

import com.aionemu.boot.i18n.I18n;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统属性安全读取与类型转换工具。
 * Safe system-property access and type conversion.
 */
@Slf4j
@UtilityClass
public class SystemPropertyUtil {

    private static boolean loggedException;
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");

    /**
     * 判断系统属性是否存在。
     * Whether a system property exists.
     *
     * Property key
     *
     * @param key 存在则为 true / True if present
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * 读取系统属性，不存在返回 null。
     * Read a system property, or null if missing.
     *
     * Property key
     * Value or null
     */
    public String get(String key) {
        return get(key, (String) null);
    }

    /**
     * 读取系统属性，不存在返回默认值。
     * Read a system property with a default.
     *
     * Property key
     * Default value
     * @return 属性值或默认值 / Value or default
     */
    public String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        } else if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        } else {
            String value = null;

            try {
                value = System.getProperty(key);
            } catch (Exception var4) {
                if (!loggedException) {
                    log(I18n.get("log.89d9ed220773", key), var4);
                    loggedException = true;
                }
            }

            return value == null ? def : value;
        }
    }

    /**
     * 读取布尔系统属性。
     * Read a boolean system property.
     *
     * Property key
     * Default value
     * @return 布尔值或默认值 / Boolean or default
     */
    public boolean getBoolean(String key, boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        } else {
            value = value.trim().toLowerCase();
            if (value.isEmpty()) {
                return true;
            } else if (!"true".equals(value) && !"yes".equals(value) && !"1".equals(value)) {
                if (!"false".equals(value) && !"no".equals(value) && !"0".equals(value)) {
                    log(I18n.get("log.2f677f096a53", key, value, def));
                    return def;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    /**
     * 读取整型系统属性。
     * Read an integer system property.
     *
     * Property key
     * Default value
     * @return 整型值或默认值 / Int or default
     */
    public int getInt(String key, int def) {
        String value = get(key);
        if (value == null) {
            return def;
        } else {
            value = value.trim().toLowerCase();
            if (INTEGER_PATTERN.matcher(value).matches()) {
                try {
                    return Integer.parseInt(value);
                } catch (Exception var4) {
                }
            }

            log(I18n.get("log.6f99708db3d4", key, value, def));
            return def;
        }
    }

    /**
     * 读取长整型系统属性。
     * Read a long system property.
     *
     * Property key
     * Default value
     * @return 长整型值或默认值 / Long or default
     */
    public long getLong(String key, long def) {
        String value = get(key);
        if (value == null) {
            return def;
        } else {
            value = value.trim().toLowerCase();
            if (INTEGER_PATTERN.matcher(value).matches()) {
                try {
                    return Long.parseLong(value);
                } catch (Exception var5) {
                }
            }

            log(I18n.get("log.9be1818fb8ef", key, value, def));
            return def;
        }
    }

    /**
     * 记录警告日志。
     * Log a warning message.
     *
     * Message
     */
    private void log(String msg) {
        log.warn(msg);
    }

    /**
     * 记录带异常的警告日志。
     * Log a warning with exception.
     *
     * Message
     * @param e   异常 / Exception
     */
    private void log(String msg, Exception e) {
        log.warn(msg, e);
    }
}
