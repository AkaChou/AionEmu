package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;

/**
 * 布尔值属性转换器
 * Boolean property transformer class that handles conversion of string values to Boolean objects.
 *
 * 支持的输入格式:
 * Supported input formats:
 * - true/false (不区分大小写 case-insensitive)
 * - 1/0
 */
public class BooleanTransformer implements PropertyTransformer<Boolean> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final BooleanTransformer SHARED_INSTANCE = new BooleanTransformer();

    /**
     * 将字符串值转换为Boolean对象
     * Transforms string value into Boolean object
     *
     * @param value 要转换的字符串值 String value to transform
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的Boolean对象 Transformed Boolean object
     * @throws TransformationException 如果输入字符串格式无效 if string value has invalid format
     */
    public Boolean transform(String value, Field field) throws TransformationException {
        if (!("true".equalsIgnoreCase(value) || "1".equals(value))) {
            if (!("false".equalsIgnoreCase(value) || "0".equals(value))) {
                throw new TransformationException("Invalid boolean string: " + value);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
