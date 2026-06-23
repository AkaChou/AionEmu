package com.aionemu.commons.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置处理器类，用于处理带有@Property注解的类字段的配置加载
 * Configuration processor class for handling configuration loading of class fields annotated with @Property
 *
 * 该类通过反射机制读取类的字段，并根据@Property注解的配置从Properties中加载对应的值
 * This class uses reflection to read class fields and load corresponding values from Properties based on @Property annotations
 *
 * @author SunAion
 */
public class ConfigurableProcessor {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableProcessor.class);

    /**
     * 处理对象或类的配置
     * Process configuration for an object or class
     *
     * @param object 要处理的对象或类
     * @param properties 配置属性数组
     */
    public static void process(Object object, Properties... properties) {
        Class clazz;
        if (object instanceof Class) {
            clazz = (Class)object;
            object = null;
        } else {
            clazz = object.getClass();
        }

        process(clazz, object, properties);
    }

    /**
     * 递归处理类的字段配置
     * Recursively process class field configurations
     *
     * @param clazz 要处理的类
     * @param obj 类的实例对象
     * @param props 配置属性数组
     */
    private static void process(Class<?> clazz, Object obj, Properties[] props) {
        processFields(clazz, obj, props);
        if (obj == null) {
            Class[] arr$ = clazz.getInterfaces();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                Class<?> itf = arr$[i$];
                process(itf, obj, props);
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            process(superClass, obj, props);
        }
    }

    /**
     * 处理类的所有带有@Property注解的字段
     * Process all fields with @Property annotation in the class
     *
     * @param clazz 要处理的类
     * @param obj 类的实例对象
     * @param props 配置属性数组
     */
    private static void processFields(Class<?> clazz, Object obj, Properties[] props) {
        Field[] arr$ = clazz.getDeclaredFields();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Field f = arr$[i$];
            if ((!Modifier.isStatic(f.getModifiers()) || obj == null) && 
                (Modifier.isStatic(f.getModifiers()) || obj != null) && 
                f.isAnnotationPresent(Property.class)) {
                if (Modifier.isFinal(f.getModifiers())) {
                    log.error("Attempt to proceed final field " + f.getName() + " of class " + clazz.getName());
                    throw new RuntimeException();
                }

                processField(f, obj, props);
            }
        }
    }

    /**
     * 处理单个字段的配置
     * Process configuration for a single field
     *
     * @param f 要处理的字段
     * @param obj 字段所属的对象
     * @param props 配置属性数组
     */
    private static void processField(Field f, Object obj, Properties[] props) {
        boolean oldAccessible = f.isAccessible();
        f.setAccessible(true);

        try {
            Property property = (Property)f.getAnnotation(Property.class);
            if ("DO_NOT_OVERWRITE_INITIALIAZION_VALUE".equals(property.defaultValue()) && 
                !isKeyPresent(property.key(), props)) {
                if (log.isDebugEnabled()) {
                    log.debug("Field " + f.getName() + " of class " + f.getDeclaringClass().getName() + " wasn't modified");
                }
            } else {
                f.set(obj, getFieldValue(f, props));
            }
        } catch (Exception var5) {
            log.error("Can't transform field " + f.getName() + " of class " + f.getDeclaringClass());
            throw new RuntimeException();
        }

        f.setAccessible(oldAccessible);
    }

    /**
     * 获取字段的配置值
     * Get configuration value for a field
     *
     * @param field 要获取值的字段
     * @param props 配置属性数组
     * @return 转换后的字段值
     * @throws TransformationException 如果值转换失败
     */
    private static Object getFieldValue(Field field, Properties[] props) throws TransformationException {
        Property property = (Property)field.getAnnotation(Property.class);
        String defaultValue = property.defaultValue();
        String key = property.key();
        String value = null;
        if (key.isEmpty()) {
            log.warn("Property " + field.getName() + " of class " + field.getDeclaringClass().getName() + " has empty key");
        } else {
            value = findPropertyByKey(key, props);
        }

        if (value == null || value.trim().equals("")) {
            value = defaultValue;
            if (log.isDebugEnabled()) {
                log.debug("Using default value for field " + field.getName() + " of class " + field.getDeclaringClass().getName());
            }
        }

        PropertyTransformer<?> pt = PropertyTransformerFactory.newTransformer(field.getType(), property.propertyTransformer());
        return pt.transform(value, field);
    }

    /**
     * 在配置属性数组中查找指定键的值
     * Find value for specified key in properties array
     *
     * @param key 要查找的键
     * @param props 配置属性数组
     * @return 找到的值，如果未找到则返回null
     */
    private static String findPropertyByKey(String key, Properties[] props) {
        Properties[] arr$ = props;
        int len$ = props.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Properties p = arr$[i$];
            if (p.containsKey(key)) {
                return p.getProperty(key);
            }
        }

        return null;
    }

    /**
     * 检查指定的键是否存在于配置属性数组中
     * Check if specified key exists in properties array
     *
     * @param key 要检查的键
     * @param props 配置属性数组
     * @return 如果键存在则返回true，否则返回false
     */
    private static boolean isKeyPresent(String key, Properties[] props) {
        return findPropertyByKey(key, props) != null;
    }
}
