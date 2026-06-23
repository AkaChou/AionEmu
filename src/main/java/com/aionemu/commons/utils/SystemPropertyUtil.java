package com.aionemu.commons.utils;

import java.util.logging.Level;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统属性工具类，提供对系统属性的安全访问和类型转换功能
 * System property utility class providing safe access and type conversion for system properties
 */
public final class SystemPropertyUtil {
   private static boolean initializedLogger = true;
   private static final Logger logger = LoggerFactory.getLogger(SystemPropertyUtil.class);
   private static boolean loggedException;
   private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");

   /**
    * 检查是否存在指定的系统属性
    * Check if a system property exists
    *
    * @param key 属性键名
    *            Property key
    * @return 如果属性存在则返回true
    *         Returns true if the property exists
    */
   public static boolean contains(String key) {
      return get(key) != null;
   }

   /**
    * 获取系统属性值
    * Get system property value
    *
    * @param key 属性键名
    *            Property key
    * @return 属性值，如果不存在则返回null
    *         Property value, or null if not exists
    */
   public static String get(String key) {
      return get(key, (String)null);
   }

   /**
    * 获取系统属性值，如果不存在则返回默认值
    * Get system property value with default value
    *
    * @param key 属性键名
    *            Property key
    * @param def 默认值
    *            Default value
    * @return 属性值或默认值
    *         Property value or default value
    */
   public static String get(String key, String def) {
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
               log("Unable to retrieve a system property '" + key + "'; default values will be used.", var4);
               loggedException = true;
            }
         }

         return value == null ? def : value;
      }
   }

   /**
    * 获取布尔类型的系统属性值
    * Get boolean system property value
    *
    * @param key 属性键名
    *            Property key
    * @param def 默认值
    *            Default value
    * @return 布尔类型的属性值或默认值
    *         Boolean property value or default value
    */
   public static boolean getBoolean(String key, boolean def) {
      String value = get(key);
      if (value == null) {
         return def;
      } else {
         value = value.trim().toLowerCase();
         if (value.isEmpty()) {
            return true;
         } else if (!"true".equals(value) && !"yes".equals(value) && !"1".equals(value)) {
            if (!"false".equals(value) && !"no".equals(value) && !"0".equals(value)) {
               log("Unable to parse the boolean system property '" + key + "':" + value + " - " + "using the default value: " + def);
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
    * 获取整数类型的系统属性值
    * Get integer system property value
    *
    * @param key 属性键名
    *            Property key
    * @param def 默认值
    *            Default value
    * @return 整数类型的属性值或默认值
    *         Integer property value or default value
    */
   public static int getInt(String key, int def) {
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

         log("Unable to parse the integer system property '" + key + "':" + value + " - " + "using the default value: " + def);
         return def;
      }
   }

   /**
    * 获取长整数类型的系统属性值
    * Get long integer system property value
    *
    * @param key 属性键名
    *            Property key
    * @param def 默认值
    *            Default value
    * @return 长整数类型的属性值或默认值
    *         Long integer property value or default value
    */
   public static long getLong(String key, long def) {
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

         log("Unable to parse the long integer system property '" + key + "':" + value + " - " + "using the default value: " + def);
         return def;
      }
   }

   private static void log(String msg) {
      if (initializedLogger) {
         logger.warn(msg);
      } else {
         java.util.logging.Logger.getLogger(SystemPropertyUtil.class.getName()).log(Level.WARNING, msg);
      }

   }

   private static void log(String msg, Exception e) {
      if (initializedLogger) {
         logger.warn(msg, e);
      } else {
         java.util.logging.Logger.getLogger(SystemPropertyUtil.class.getName()).log(Level.WARNING, msg, e);
      }

   }

   private SystemPropertyUtil() {
   }
}
