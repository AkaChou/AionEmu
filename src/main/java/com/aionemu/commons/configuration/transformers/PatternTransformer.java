package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * 正则表达式属性转换器
 * Pattern property transformer class that handles conversion of string values to Pattern objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 有效的正则表达式字符串 valid regular expression string
 */
public class PatternTransformer implements PropertyTransformer<Pattern> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final PatternTransformer SHARED_INSTANCE = new PatternTransformer();

    /**
     * 将字符串值转换为Pattern对象
     * Transforms string value into Pattern object
     *
     * @param value 要转换的字符串值（正则表达式）String value to transform (regular expression)
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Pattern对象 Transformed Pattern object
     * @throws TransformationException 如果正则表达式无效 if regular expression is invalid
     */
    public Pattern transform(String value, Field field) throws TransformationException {
        try {
            return Pattern.compile(value);
        } catch (Exception var4) {
            throw new TransformationException("Not valid RegExp: " + value, var4);
        }
    }
}
