package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 单精度浮点数属性转换器
 * Float property transformer class that handles conversion of string values to Float objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 十进制数字 decimal numbers
 * - 科学计数法 scientific notation (e.g., 1.23E-4)
 */
public class FloatTransformer implements PropertyTransformer<Float> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final FloatTransformer SHARED_INSTANCE = new FloatTransformer();

    /**
     * 将字符串值转换为Float对象
     * Transforms string value into Float object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Float对象 Transformed Float object
     * @throws TransformationException 如果转换失败 if transformation fails
     */
    public Float transform(String value, Field field) throws TransformationException {
        try {
            return Float.parseFloat(value);
        } catch (Exception var4) {
            throw new TransformationException(var4);
        }
    }
}
