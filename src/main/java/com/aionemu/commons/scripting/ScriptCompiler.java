package com.aionemu.commons.scripting;

import java.io.File;

/**
 * 脚本编译器接口，定义了编译脚本所需的基本方法
 * Script compiler interface that defines basic methods for compiling scripts
 *
 * @author SunAion Team
 */
public interface ScriptCompiler {
    /**
     * 设置父类加载器
     * Set parent class loader
     *
     * @param classLoader 父类加载器 / Parent class loader
     */
    void setParentClassLoader(ScriptClassLoader classLoader);

    /**
     * 设置编译所需的库文件
     * Set libraries needed for compilation
     *
     * @param libraries 库文件集合 / Collection of library files
     */
    void setLibraires(Iterable<File> libraries);

    /**
     * 编译单个类
     * Compile a single class
     *
     * @param className 类名 / Class name
     * @param sourceCode 源代码 / Source code
     * @return 编译结果 / Compilation result
     */
    CompilationResult compile(String className, String sourceCode);

    /**
     * 编译多个类
     * Compile multiple classes
     *
     * @param classNames 类名数组 / Array of class names
     * @param sourceCode 源代码数组 / Array of source codes
     * @return 编译结果 / Compilation result
     * @throws IllegalArgumentException 如果类名和源代码数量不匹配 / If number of class names and source codes don't match
     */
    CompilationResult compile(String[] classNames, String[] sourceCode) throws IllegalArgumentException;

    /**
     * 编译指定文件集合中的源代码
     * Compile source code from specified files
     *
     * @param files 源代码文件集合 / Collection of source code files
     * @return 编译结果 / Compilation result
     */
    CompilationResult compile(Iterable<File> files);

    /**
     * 获取支持的文件类型
     * Get supported file types
     *
     * @return 支持的文件类型数组 / Array of supported file types
     */
    String[] getSupportedFileTypes();
}
