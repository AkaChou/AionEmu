package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 字符串属性转换器
 * String property transformer class that handles conversion of string values to String objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 任意字符串 any string value
 */
public class StringTransformer implements PropertyTransformer<String> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final StringTransformer SHARED_INSTANCE = new StringTransformer();

    /**
     * 将字符串值转换为String对象
     * Transforms string value into String object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的String对象 Transformed String object
     * @throws TransformationException 如果转换失败 if transformation fails
     */
    public String transform(String value, Field field) throws TransformationException {
        return value;
    }
}
