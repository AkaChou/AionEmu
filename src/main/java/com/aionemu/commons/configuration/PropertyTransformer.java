package com.aionemu.commons.configuration;

import java.lang.reflect.Field;

/**
 * 属性值转换器接口，用于将配置字符串转换为指定类型的值
 * Property value transformer interface for converting configuration strings to values of specified types
 *
 * 该接口定义了配置值转换的标准方法，所有具体的转换器实现都需要实现此接口
 * This interface defines the standard method for configuration value transformation,
 * all concrete transformer implementations need to implement this interface
 *
 * @param <T> 转换后的值类型 The type of transformed value
 * @author SunAion
 */
public interface PropertyTransformer<T> {
    /**
     * 将配置字符串转换为指定类型的值
     * Transform configuration string to value of specified type
     *
     * @param value 要转换的配置字符串值 Configuration string value to transform
     * @param field 要设置值的字段 Field to set the value for
     * @return 转换后的值 Transformed value
     * @throws TransformationException 如果转换过程中发生错误 If error occurs during transformation
     */
    T transform(String value, Field field) throws TransformationException;
}
