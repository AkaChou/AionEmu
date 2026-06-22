package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 枚举属性转换器
 * Enum property transformer class that handles conversion of string values to Enum objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 枚举常量名称 enum constant name (case-sensitive)
 */
public class EnumTransformer implements PropertyTransformer<Enum<?>> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final EnumTransformer SHARED_INSTANCE = new EnumTransformer();

    /**
     * 将字符串值转换为Enum对象
     * Transforms string value into Enum object
     *
     * @param value 要转换的字符串值（枚举常量名称）String value to transform (enum constant name)
     * @param field 字段对象（必须是枚举类型）Field that will be transformed (must be an enum type)
     * @return 转换后的Enum对象 Transformed Enum object
     * @throws TransformationException 如果找不到指定的枚举常量 if specified enum constant cannot be found
     */
    public Enum<?> transform(String value, Field field) throws TransformationException {
        Class<?> clazz = field.getType();

        try {
            return Enum.valueOf((Class<Enum>)clazz, value);
        } catch (Exception var5) {
            throw new TransformationException(var5);
        }
    }
}
