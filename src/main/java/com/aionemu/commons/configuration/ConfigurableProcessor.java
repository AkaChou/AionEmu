package com.aionemu.commons.configuration;


import com.aionemu.boot.i18n.I18n;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * 配置处理器类，用于处理带有@Property 注解的类字段的配置加载
 * Configuration processor class for handling configuration loading of class fields annotated with @Property
 * 该类通过反射机制读取类的字段，并根据@Property 注解的配置从 Properties 中加载对应的值
 * This class uses reflection to read class fields and load corresponding values from Properties based on @Property annotations
 * @author SunAion
 */
@Slf4j
public class ConfigurableProcessor {

    /**
     * `.value` 规范化子键后缀：标量配置迁移为「标量+子键」形态后，文件键统一落在该后缀下，
     * 裸键仅在 Environment/Binder 侧存在，遗留装配需对称回退探测。
     * Suffix of the normalized `.value` sub-key: once a scalar config migrates to the
     * "scalar + sub-key" shape, file keys live under this suffix while the bare key only exists
     * on the Environment/Binder side, so the legacy assembly must probe it symmetrically.
     */
    private static final String VALUE_SUB_KEY_SUFFIX = ".value";

    /**
     * 处理对象或类的配置
     * Process configuration for an object or class
     * @param object 要处理的对象或类
     * @param properties 配置属性数组
     */
    public static void process(Object object, Properties... properties) {
        Class<?> clazz;
        Object owner;
        if (object instanceof Class<?> c) {
            clazz = c;
            owner = null;
        } else {
            clazz = object.getClass();
            owner = object;
        }

        process(clazz, owner, properties);
    }

    /**
     * 递归处理类的字段配置
     * Recursively process class field configurations
     * @param clazz 要处理的类
     * @param obj 类的实例对象
     * @param props 配置属性数组
     */
    private static void process(Class<?> clazz, Object obj, Properties[] props) {
        processFields(clazz, obj, props);
        if (obj == null) {
            for (Class<?> itf : clazz.getInterfaces()) {
                process(itf, obj, props);
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            process(superClass, obj, props);
        }
    }

    /**
     * 处理类的所有带有@Property 注解的字段
     * Process all fields with @Property annotation in the class
     * @param clazz 要处理的类
     * @param obj 类的实例对象
     * @param props 配置属性数组
     */
    private static void processFields(Class<?> clazz, Object obj, Properties[] props) {
        for (Field f : clazz.getDeclaredFields()) {
            boolean isStatic = Modifier.isStatic(f.getModifiers());
            // 静态字段仅在处理类（obj == null）时写入，实例字段仅在处理实例时写入。
            // Static fields are written only when processing the class (obj == null), instance fields only when processing an instance.
            if (isStatic == (obj == null) && f.isAnnotationPresent(Property.class)) {
                if (Modifier.isFinal(f.getModifiers())) {
                    log.error(I18n.get("log.9ad54acf6b37", f.getName(), clazz.getName()));
                    throw new RuntimeException("Final field with @Property: " + clazz.getName() + "." + f.getName());
                }

                processField(f, obj, props);
            }
        }
    }

    /**
     * 处理单个字段的配置
     * Process configuration for a single field
     * @param f 要处理的字段
     * @param obj 字段所属的对象
     * @param props 配置属性数组
     */
    private static void processField(Field f, Object obj, Properties[] props) {
        boolean oldAccessible = f.canAccess(obj);
        f.setAccessible(true);

        try {
            Property property = f.getAnnotation(Property.class);
            if ("DO_NOT_OVERWRITE_INITIALIAZION_VALUE".equals(property.defaultValue()) &&
                !isKeyPresent(property.key(), props)) {
                log.debug("Field {} of class {} wasn't modified", f.getName(), f.getDeclaringClass().getName());
            } else {
                f.set(obj, getFieldValue(f, props));
            }
        } catch (Exception e) {
            log.error(I18n.get("log.de0d5400364f", f.getName(), f.getDeclaringClass()), e);
            throw new RuntimeException(e);
        }

        f.setAccessible(oldAccessible);
    }

    /**
     * 获取字段的配置值
     * Get configuration value for a field
     * @param field 要获取值的字段
     * @param props 配置属性数组
     * @return 转换后的字段值 / transformed field value
     * @throws TransformationException 如果值转换失败
     */
    private static Object getFieldValue(Field field, Properties[] props) throws TransformationException {
        Property property = field.getAnnotation(Property.class);
        String defaultValue = property.defaultValue();
        String key = property.key();
        String value = null;
        if (key.isEmpty()) {
            log.warn(I18n.get("log.9815dac71108", field.getName(), field.getDeclaringClass().getName()));
        } else {
            // 先取启动层发布的高优先级来源（命令行/环境变量/application.yml），
            // 保证 Bean 绑定与静态字段最终看到同一个值。
            // Consult the bootstrap-published higher-precedence source first so bean binding and the
            // static fields end up on the same value.
            value = ConfigSourceResolverHolder.resolve(key);
            if (value == null) {
                value = findPropertyByKey(key, props);
            }
            if (value == null) {
                // 裸键在两个通道均缺失时回退探测 `.value` 规范化子键，
                // 防止 defaultValue 覆盖 Binder 已绑定的文件值。
                // Probe the normalized `.value` sub-key when the bare key is missing on both
                // channels so the defaultValue cannot overwrite the bound file value.
                value = ConfigSourceResolverHolder.resolve(key + VALUE_SUB_KEY_SUFFIX);
                if (value == null) {
                    value = findPropertyByKey(key + VALUE_SUB_KEY_SUFFIX, props);
                }
            }
        }

        if (value == null || value.trim().isEmpty()) {
            value = defaultValue;
            log.debug("Using default value for field {} of class {}", field.getName(), field.getDeclaringClass().getName());
        }

        PropertyTransformer<?> pt = PropertyTransformerFactory.newTransformer(field.getType(), property.propertyTransformer());
        return pt.transform(value, field);
    }

    /**
     * 在配置属性数组中查找指定键的值
     * Find value for specified key in properties array
     * @param key 要查找的键
     * @param props 配置属性数组
     * @return 找到的值，如果未找到则返回 null / found value, or null if not found
     */
    private static String findPropertyByKey(String key, Properties[] props) {
        for (Properties p : props) {
            if (p.containsKey(key)) {
                return p.getProperty(key);
            }
        }

        return null;
    }

    /**
     * 检查指定的键是否存在于配置属性数组中
     * Check if specified key exists in properties array
     * @param key 要检查的键
     * @param props 配置属性数组
     * @return 如果键存在则返回 true，否则返回 false / true if the key exists, false otherwise
     */
    private static boolean isKeyPresent(String key, Properties[] props) {
        // 与 getFieldValue 的 `.value` 子键回退保持闭环，否则文件只写规范化键时
        // DO_NOT_OVERWRITE 字段会被误判为「文件未提供值」。
        // Mirror the `.value` sub-key fallback in getFieldValue, otherwise a file carrying only
        // the normalized key makes DO_NOT_OVERWRITE fields look unset.
        return findPropertyByKey(key, props) != null
            || findPropertyByKey(key + VALUE_SUB_KEY_SUFFIX, props) != null;
    }
}
