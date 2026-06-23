package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 双精度浮点数属性转换器
 * Double property transformer class that handles conversion of string values to Double objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 十进制数字 decimal numbers
 * - 科学计数法 scientific notation (e.g., 1.23E-4)
 */
public class DoubleTransformer implements PropertyTransformer<Double> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final DoubleTransformer SHARED_INSTANCE = new DoubleTransformer();

    /**
     * 将字符串值转换为Double对象
     * Transforms string value into Double object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Double对象 Transformed Double object
     * @throws TransformationException 如果转换失败 if transformation fails
     */
    public Double transform(String value, Field field) throws TransformationException {
        try {
            return Double.parseDouble(value);
        } catch (Exception var4) {
            throw new TransformationException(var4);
        }
    }
}
