package com.aionemu.commons.scripting;

import java.util.Arrays;

/**
 * 编译结果类，用于存储脚本编译后的类信息和类加载器
 * Compilation result class for storing compiled script classes and their class loader
 *
 * @author SunAion Team
 */
public class CompilationResult {
    /** 
     * 已编译的类数组
     * Array of compiled classes 
     */
    private final Class<?>[] compiledClasses;

    /** 
     * 用于加载编译后类的类加载器
     * Class loader for loading the compiled classes 
     */
    private final ScriptClassLoader classLoader;

    /**
     * 构造函数，初始化编译结果
     * Constructor to initialize compilation result
     *
     * @param compiledClasses 编译后的类数组 / Array of compiled classes
     * @param classLoader 用于加载这些类的类加载器 / Class loader for these classes
     */
    public CompilationResult(Class<?>[] compiledClasses, ScriptClassLoader classLoader) {
        this.compiledClasses = compiledClasses;
        this.classLoader = classLoader;
   }

    /**
     * 获取类加载器
     * Get the class loader
     *
     * @return 用于加载编译类的类加载器 / Class loader for compiled classes
     */
    public ScriptClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * 获取编译后的类数组
     * Get the array of compiled classes
     *
     * @return 编译后的类数组 / Array of compiled classes
     */
    public Class<?>[] getCompiledClasses() {
        return this.compiledClasses;
    }

    /**
     * 转换为字符串表示
     * Convert to string representation
     *
     * @return 包含类加载器和编译类信息的字符串 / String containing class loader and compiled classes info
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompilationResult");
        sb.append("{classLoader=").append(this.classLoader);
        sb.append(", compiledClasses=").append(this.compiledClasses == null ? "null" : Arrays.asList(this.compiledClasses).toString());
        sb.append('}');
        return sb.toString();
    }
}
