package com.aionemu.commons.scripting;

import com.aionemu.commons.scripting.classlistener.ClassListener;
import java.io.File;
import java.util.Collection;

/**
 * 脚本上下文接口，定义了脚本生命周期管理和配置的方法
 * Script context interface that defines methods for script lifecycle management and configuration
 *
 * @author SunAion Team
 */
public interface ScriptContext {
    /**
     * 初始化脚本上下文
     * Initialize the script context
     */
    void init();

    /**
     * 关闭脚本上下文
     * Shutdown the script context
     */
    void shutdown();

    /**
     * 重新加载脚本
     * Reload the scripts
     */
    void reload();

    /**
     * 获取根目录
     * Get the root directory
     *
     * @return 脚本根目录 / Script root directory
     */
    File getRoot();

    /**
     * 获取编译结果
     * Get the compilation result
     *
     * @return 脚本编译结果 / Script compilation result
     */
    CompilationResult getCompilationResult();

    /**
     * 检查是否已初始化
     * Check if initialized
     *
     * @return 是否已初始化 / Whether initialized
     */
    boolean isInitialized();

    /**
     * 设置库文件
     * Set library files
     *
     * @param libraries 库文件集合 / Collection of library files
     */
    void setLibraries(Iterable<File> libraries);

    /**
     * 获取库文件
     * Get library files
     *
     * @return 库文件集合 / Collection of library files
     */
    Iterable<File> getLibraries();

    /**
     * 获取父脚本上下文
     * Get parent script context
     *
     * @return 父脚本上下文 / Parent script context
     */
    ScriptContext getParentScriptContext();

    /**
     * 获取子脚本上下文集合
     * Get child script contexts
     *
     * @return 子脚本上下文集合 / Collection of child script contexts
     */
    Collection<ScriptContext> getChildScriptContexts();

    /**
     * 添加子脚本上下文
     * Add child script context
     *
     * @param context 子脚本上下文 / Child script context
     */
    void addChildScriptContext(ScriptContext context);

    /**
     * 设置类监听器
     * Set class listener
     *
     * @param listener 类监听器 / Class listener
     */
    void setClassListener(ClassListener listener);

    /**
     * 获取类监听器
     * Get class listener
     *
     * @return 类监听器 / Class listener
     */
    ClassListener getClassListener();

    /**
     * 设置编译器类名
     * Set compiler class name
     *
     * @param className 编译器类名 / Compiler class name
     */
    void setCompilerClassName(String className);

    /**
     * 获取编译器类名
     * Get compiler class name
     *
     * @return 编译器类名 / Compiler class name
     */
    String getCompilerClassName();

   boolean equals(Object var1);

   int hashCode();

   void finalize() throws Throwable;
}
