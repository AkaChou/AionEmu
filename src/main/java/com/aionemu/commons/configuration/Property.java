package com.aionemu.commons.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置属性注解，用于标记需要从配置文件中加载值的字段
 * Configuration property annotation used to mark fields that need to load values from configuration files
 *
 * 该注解提供了配置项的键名、默认值和值转换器的定义
 * This annotation provides definitions for configuration key name, default value and value transformer
 *
 * @author SunAion
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
    /**
     * 默认值常量，表示不覆盖字段的初始值
     * Default value constant indicating not to overwrite field's initial value
     */
    String DEFAULT_VALUE = "DO_NOT_OVERWRITE_INITIALIAZION_VALUE";

    /**
     * 配置项的键名
     * Configuration item key name
     *
     * @return 配置键名
     */
    String key();

    /**
     * 值转换器类，用于将配置字符串转换为字段类型
     * Value transformer class used to convert configuration string to field type
     *
     * @return 转换器类
     */
    Class<? extends PropertyTransformer> propertyTransformer() default PropertyTransformer.class;

    /**
     * 配置项的默认值
     * Default value for configuration item
     *
     * @return 默认值字符串
     */
    String defaultValue() default DEFAULT_VALUE;
}
