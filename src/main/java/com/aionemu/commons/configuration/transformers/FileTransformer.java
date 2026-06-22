package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.io.File;
import java.lang.reflect.Field;

/**
 * 文件属性转换器
 * File property transformer class that handles conversion of string values to File objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - 文件路径字符串 file path string
 */
public class FileTransformer implements PropertyTransformer<File> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final FileTransformer SHARED_INSTANCE = new FileTransformer();

    /**
     * 将字符串值转换为File对象
     * Transforms string value into File object
     *
     * @param value 要转换的字符串值（文件路径）String value to transform (file path)
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的File对象 Transformed File object
     * @throws TransformationException 如果文件路径无效 if file path is invalid
     */
    public File transform(String value, Field field) throws TransformationException {
        return new File(value);
    }
}
