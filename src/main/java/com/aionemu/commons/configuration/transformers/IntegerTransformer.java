package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 整数属性转换器
 * Integer property transformer class that handles conversion of string values to Integer objects.
 *
 * 支持的输入格式:
 * Supported input formats:
 * - 十进制数字 decimal numbers
 * - 十六进制数字(0x前缀) hexadecimal numbers (0x prefix)
 * - 八进制数字(0前缀) octal numbers (0 prefix)
 */
public class IntegerTransformer implements PropertyTransformer<Integer> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final IntegerTransformer SHARED_INSTANCE = new IntegerTransformer();

    /**
     * 将字符串值转换为Integer对象
     * Transforms string value into Integer object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Integer对象 Transformed Integer object
     * @throws TransformationException 如果转换失败 if transformation fails
     */
    public Integer transform(String value, Field field) throws TransformationException {
        try {
            return Integer.decode(value);
        } catch (Exception var4) {
            throw new TransformationException(var4);
        }
    }
}
