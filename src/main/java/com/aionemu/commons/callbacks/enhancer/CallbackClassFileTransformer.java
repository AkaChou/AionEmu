package com.aionemu.commons.callbacks.enhancer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字节码转换器基类，用于实现回调功能的字节码增强
 * Base class for bytecode transformer that implements callback functionality enhancement
 *
 * 该类实现了Java的ClassFileTransformer接口，提供了类加载时的字节码转换功能
 * This class implements Java's ClassFileTransformer interface to provide bytecode transformation during class loading
 *
 * 主要功能:
 * Main features:
 * 1. 过滤系统类加载器加载的类 / Filter classes loaded by system class loader
 * 2. 提供字节码转换的统一入口 / Provide unified entry point for bytecode transformation
 * 3. 处理转换过程中的异常 / Handle exceptions during transformation process
 */
public abstract class CallbackClassFileTransformer implements ClassFileTransformer {
    
    /**
     * 日志记录器
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(CallbackClassFileTransformer.class);

    /**
     * 实现ClassFileTransformer接口的transform方法
     * Implements the transform method of ClassFileTransformer interface
     *
     * @param loader 类加载器 / Class loader
     * @param className 类名 / Class name
     * @param classBeingRedefined 重定义的类 / Class being redefined
     * @param protectionDomain 保护域 / Protection domain
     * @param classfileBuffer 类文件字节码 / Class file bytecode
     * @return 转换后的字节码，如果不需要转换则返回null / Transformed bytecode, or null if no transformation needed
     * @throws IllegalClassFormatException 如果字节码格式非法 / If bytecode format is illegal
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            // 跳过ExtClassLoader加载的系统类
            // Skip system classes loaded by ExtClassLoader
            if (loader != null && !loader.getClass().getName().equals("sun.misc.Launcher$ExtClassLoader")) {
                return this.transformClass(loader, classfileBuffer);
            } else {
                log.trace("Class " + className + " ignored.");
                return null;
            }
        } catch (Exception var8) {
            Error e1 = new Error("Can't transform class " + className, var8);
            log.error(e1.getMessage(), e1);
            // AppClassLoader加载失败时强制退出
            // Force exit when AppClassLoader fails to load
            if (loader.getClass().getName().equals("sun.misc.Launcher$AppClassLoader")) {
                Runtime.getRuntime().halt(1);
            }
            throw e1;
        }
    }

    /**
     * 执行实际的类转换操作，由子类实现具体的转换逻辑
     * Perform actual class transformation, concrete transformation logic to be implemented by subclasses
     *
     * @param loader 类加载器 / Class loader
     * @param classfileBuffer 类文件字节码 / Class file bytecode
     * @return 转换后的字节码 / Transformed bytecode
     * @throws Exception 转换过程中的异常 / Exception during transformation
     */
    protected abstract byte[] transformClass(ClassLoader loader, byte[] classfileBuffer) throws Exception;
}
