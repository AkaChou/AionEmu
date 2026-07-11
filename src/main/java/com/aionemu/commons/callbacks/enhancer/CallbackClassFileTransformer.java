package com.aionemu.commons.callbacks.enhancer;

import lombok.extern.slf4j.Slf4j;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import com.aionemu.commons.utils.AionProcessExit;

/**
 * 字节码转换器基类，用于实现回调功能的字节码增强
 * Base class for bytecode transformer that implements callback functionality enhancement
 *
 * 该类实现了 Java 的 ClassFileTransformer 接口，提供了类加载时的字节码转换功能
 * This class implements Java's ClassFileTransformer interface to provide bytecode transformation during class loading
 *
 * 主要功能:
 * Main features:
 * 1. 过滤系统类加载器加载的类 / Filter classes loaded by system class loader
 * 2. 提供字节码转换的统一入口 / Provide unified entry point for bytecode transformation
 * 3. 处理转换过程中的异常 / Handle exceptions during transformation process
 */
@Slf4j
public abstract class CallbackClassFileTransformer implements ClassFileTransformer {
    

    /**
 * 实现 ClassFileTransformer 接口的 transform 方法
     * Implements the transform method of ClassFileTransformer interface
     *
     * Class loader
     * Class name
     * @param classBeingRedefined 重定义的类 / Class being redefined
     * Protection domain
     * @param classfileBuffer 类文件字节码 / Class file bytecode
     * Transformed bytecode, or null if no transformation needed
     * If bytecode format is illegal。 / If bytecode format is illegal.
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            // 跳过平台/引导类加载器加载的系统类
            // 跳过平台/引导类加载器加载的系统类 / Skip system classes loaded by the platform/bootstrap class loaders
            if (shouldTransform(loader)) {
                return this.transformClass(loader, classfileBuffer);
            } else {
                log.trace("Class " + className + " ignored.");
                return null;
            }
        } catch (Exception var8) {
            Error e1 = new Error("Can't transform class " + className, var8);
            log.error(e1.getMessage(), e1);
            // 系统类加载器加载失败时强制退出
            // 系统类加载器加载失败时强制退出 / Force exit when the system class loader fails to load
            if (isSystemClassLoader(loader)) {
                AionProcessExit.halt(1);
            }
            throw e1;
        }
    }

    /**
     * 判断是否应对该 ClassLoader 下的类执行增强
     * Decide whether classes of this ClassLoader should be transformed
     *
     * Class loader
     *
     * @param loader @return 需要增强返回 true / True when transformation should run
     */
    private boolean shouldTransform(ClassLoader loader) {
        return loader != null && loader != ClassLoader.getPlatformClassLoader();
    }

    /**
     * 判断是否为系统 ClassLoader
     * Check whether the loader is the system ClassLoader
     *
     * Class loader
     *
     * @param loader @return 是系统加载器返回 true / True if it is the system class loader
     */
    private boolean isSystemClassLoader(ClassLoader loader) {
        return loader != null && loader == ClassLoader.getSystemClassLoader();
    }

    /**
     * 执行实际的类转换操作，由子类实现具体的转换逻辑
     * Perform actual class transformation, concrete transformation logic to be implemented by subclasses
     *
     * Class loader
     * @param classfileBuffer 类文件字节码 / Class file bytecode
     * @return 转换后的字节码 / Transformed bytecode
     * Exception during transformation。 / Exception during transformation.
     */
    protected abstract byte[] transformClass(ClassLoader loader, byte[] classfileBuffer) throws Exception;
}
