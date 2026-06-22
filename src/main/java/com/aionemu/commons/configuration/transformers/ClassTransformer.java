package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 类属性转换器
 * Class property transformer that handles conversion of string values to Class objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 完整的类名 fully qualified class name
 */
public class ClassTransformer implements PropertyTransformer<Class<?>> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final ClassTransformer SHARED_INSTANCE = new ClassTransformer();

    /**
     * 将字符串值转换为Class对象
     * Transforms string value into Class object
     *
     * @param value 要转换的字符串值（类的完整名称）String value to transform (fully qualified class name)
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Class对象 Transformed Class object
     * @throws TransformationException 如果找不到指定的类 if specified class cannot be found
     */
    public Class<?> transform(String value, Field field) throws TransformationException {
        try {
            return Class.forName(value, false, this.getClass().getClassLoader());
        } catch (ClassNotFoundException var4) {
            throw new TransformationException("Cannot find class with name '" + value + "'");
        }
    }
}
