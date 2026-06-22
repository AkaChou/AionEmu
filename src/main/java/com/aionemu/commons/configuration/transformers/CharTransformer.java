package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 字符属性转换器
 * Character property transformer class that handles conversion of string values to Character objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 单个字符 single character
 */
public class CharTransformer implements PropertyTransformer<Character> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final CharTransformer SHARED_INSTANCE = new CharTransformer();

    /**
     * 将字符串值转换为Character对象
     * Transforms string value into Character object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Character对象 Transformed Character object
     * @throws TransformationException 如果字符串包含多个字符 if string contains multiple characters
     */
    public Character transform(String value, Field field) throws TransformationException {
        try {
            char[] chars = value.toCharArray();
            if (chars.length > 1) {
                throw new TransformationException("Too many characters in the value");
            } else {
                return chars[0];
            }
        } catch (Exception var4) {
            throw new TransformationException(var4);
        }
    }
}
