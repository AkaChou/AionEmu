package com.aionemu.commons.scripting.impl;

import com.aionemu.commons.scripting.CompilationResult;
import com.aionemu.commons.scripting.ScriptCompiler;
import com.aionemu.commons.scripting.ScriptContext;
import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本上下文实现类，负责管理脚本的生命周期，包括初始化、关闭和重新加载等功能
 * Script context implementation class responsible for managing script lifecycle,
 * including initialization, shutdown and reloading
 *
 * @author SunAion Team
 */
public class ScriptContextImpl implements ScriptContext {
    /** 
     * 日志记录器 / Logger instance 
     */
    private static final Logger log = LoggerFactory.getLogger(ScriptContextImpl.class);
    
    /** 
     * 父脚本上下文 / Parent script context 
     */
    private final ScriptContext parentScriptContext;
    
    /** 
     * 脚本依赖的库文件 / Library files that scripts depend on 
     */
    private Iterable<File> libraries;
    
    /** 
     * 脚本根目录 / Script root directory 
     */
    private final File root;
    
    /** 
     * 脚本编译结果 / Script compilation result 
     */
    private CompilationResult compilationResult;
    
    /** 
     * 子脚本上下文集合 / Collection of child script contexts 
     */
    private Set<ScriptContext> childScriptContexts;
    
    /** 
     * 类加载监听器 / Class load listener 
     */
    private ClassListener classListener;
    
    /** 
     * 编译器类名 / Compiler class name 
     */
    private String compilerClassName;

    /**
     * 创建脚本上下文实例
     * Create script context instance
     *
     * @param root 脚本根目录 / Script root directory
     */
    public ScriptContextImpl(File root) {
        this(root, null);
    }

    /**
     * 创建脚本上下文实例
     * Create script context instance
     *
     * @param root 脚本根目录 / Script root directory
     * @param parent 父脚本上下文 / Parent script context
     * @throws NullPointerException 如果根目录为空 / If root directory is null
     * @throws IllegalArgumentException 如果根目录不存在或不是目录 / If root directory not exists or is not a directory
     */
    public ScriptContextImpl(File root, ScriptContext parent) {
        if (root == null) {
            throw new NullPointerException("Root file must be specified");
        } else if (root.exists() && root.isDirectory()) {
            this.root = root;
            this.parentScriptContext = parent;
        } else {
            throw new IllegalArgumentException("Root directory not exists or is not a directory");
        }
    }

    /**
     * 初始化脚本上下文，编译脚本文件并加载类
     * Initialize script context, compile script files and load classes
     */
    public synchronized void init() {
        if (this.compilationResult != null) {
            log.error("Init request on initialized ScriptContext");
        } else {
            ScriptCompiler scriptCompiler = this.instantiateCompiler();
            Collection<File> files = FileUtils.listFiles(this.root, scriptCompiler.getSupportedFileTypes(), true);
            if (this.parentScriptContext != null) {
                scriptCompiler.setParentClassLoader(this.parentScriptContext.getCompilationResult().getClassLoader());
            }

            scriptCompiler.setLibraires(this.libraries);
            this.compilationResult = scriptCompiler.compile(files);
            this.getClassListener().postLoad(this.compilationResult.getCompiledClasses());
            if (this.childScriptContexts != null) {
                Iterator i$ = this.childScriptContexts.iterator();

                while(i$.hasNext()) {
                    ScriptContext context = (ScriptContext)i$.next();
                    context.init();
                }
            }
        }
    }

    /**
     * 关闭脚本上下文，卸载类并清理资源
     * Shutdown script context, unload classes and clean up resources
     */
    public synchronized void shutdown() {
        if (this.compilationResult == null) {
            log.error("Shutdown of not initialized script context", new Exception());
        } else {
            if (this.childScriptContexts != null) {
                Iterator i$ = this.childScriptContexts.iterator();

                while(i$.hasNext()) {
                    ScriptContext child = (ScriptContext)i$.next();
                    child.shutdown();
                }
            }

            this.getClassListener().preUnload(this.compilationResult.getCompiledClasses());
            this.compilationResult = null;
        }
    }

    /**
     * 重新加载脚本上下文
     * Reload script context
     */
    public void reload() {
        this.shutdown();
        this.init();
    }

    /**
     * 获取脚本根目录
     * Get script root directory
     *
     * @return 脚本根目录 / Script root directory
     */
    public File getRoot() {
        return this.root;
    }

    /**
     * 获取编译结果
     * Get compilation result
     *
     * @return 编译结果 / Compilation result
     */
    public CompilationResult getCompilationResult() {
        return this.compilationResult;
    }

    /**
     * 检查是否已初始化
     * Check if initialized
     *
     * @return 是否已初始化 / Whether initialized
     */
    public synchronized boolean isInitialized() {
        return this.compilationResult != null;
    }

    /**
     * 设置库文件
     * Set library files
     *
     * @param files 库文件集合 / Collection of library files
     */
    public void setLibraries(Iterable<File> files) {
        this.libraries = files;
    }

    /**
     * 获取库文件
     * Get library files
     *
     * @return 库文件集合 / Collection of library files
     */
    public Iterable<File> getLibraries() {
        return this.libraries;
    }

    /**
     * 获取父脚本上下文
     * Get parent script context
     *
     * @return 父脚本上下文 / Parent script context
     */
    public ScriptContext getParentScriptContext() {
        return this.parentScriptContext;
    }

    /**
     * 获取子脚本上下文集合
     * Get child script contexts
     *
     * @return 子脚本上下文集合 / Collection of child script contexts
     */
    public Collection<ScriptContext> getChildScriptContexts() {
        return this.childScriptContexts;
    }

    /**
     * 添加子脚本上下文
     * Add child script context
     *
     * @param context 子脚本上下文 / Child script context
     */
    public void addChildScriptContext(ScriptContext context) {
        synchronized(this) {
            if (this.childScriptContexts == null) {
                this.childScriptContexts = new HashSet();
            }

            if (this.childScriptContexts.contains(context)) {
                log.error("Double child definition, root: " + this.root.getAbsolutePath() + ", child: " + context.getRoot().getAbsolutePath());
                return;
            }

            if (this.isInitialized()) {
                context.init();
            }
        }

        this.childScriptContexts.add(context);
    }

    /**
     * 设置类监听器
     * Set class listener
     *
     * @param cl 类监听器 / Class listener
     */
    public void setClassListener(ClassListener cl) {
        this.classListener = cl;
    }

    /**
     * 获取类监听器，如果未设置则创建默认监听器
     * Get class listener, create default listener if not set
     *
     * @return 类监听器 / Class listener
     */
    public ClassListener getClassListener() {
        if (this.classListener == null) {
            if (this.getParentScriptContext() == null) {
                AggregatedClassListener acl = new AggregatedClassListener();
                acl.addClassListener(new OnClassLoadUnloadListener());
                acl.addClassListener(new ScheduledTaskClassListener());
                this.setClassListener(acl);
                return this.classListener;
            } else {
                return this.getParentScriptContext().getClassListener();
            }
        } else {
            return this.classListener;
        }
    }

    /**
     * 设置编译器类名
     * Set compiler class name
     *
     * @param className 编译器类名 / Compiler class name
     */
    public void setCompilerClassName(String className) {
        this.compilerClassName = className;
    }

    /**
     * 获取编译器类名
     * Get compiler class name
     *
     * @return 编译器类名 / Compiler class name
     */
    public String getCompilerClassName() {
        return this.compilerClassName;
    }

    /**
     * 实例化脚本编译器
     * Instantiate script compiler
     *
     * @return 脚本编译器实例 / Script compiler instance
     * @throws RuntimeException 如果创建编译器实例失败 / If failed to create compiler instance
     */
    protected ScriptCompiler instantiateCompiler() throws RuntimeException {
        ClassLoader cl = this.getClass().getClassLoader();
        if (this.getParentScriptContext() != null) {
            cl = this.getParentScriptContext().getCompilationResult().getClassLoader();
        }

        try {
            ScriptCompiler sc = (ScriptCompiler)Class.forName(this.getCompilerClassName(), true, cl).newInstance();
            return sc;
        } catch (Exception var4) {
            log.error("Can't create instance of compiler");
            throw new RuntimeException(var4);
        }
    }

    /**
     * 比较两个脚本上下文是否相等
     * Compare if two script contexts are equal
     *
     * @param obj 要比较的对象 / Object to compare
     * @return 是否相等 / Whether equal
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ScriptContextImpl)) {
            return false;
        } else {
            ScriptContextImpl another = (ScriptContextImpl)obj;
            if (this.parentScriptContext == null) {
                return another.getRoot().equals(this.root);
            } else {
                return another.getRoot().equals(this.root) && this.parentScriptContext.equals(another.parentScriptContext);
            }
        }
    }

    /**
     * 获取哈希码
     * Get hash code
     *
     * @return 哈希码 / Hash code
     */
    public int hashCode() {
        int result = this.parentScriptContext != null ? this.parentScriptContext.hashCode() : 0;
        result = 31 * result + this.root.hashCode();
        return result;
    }

    /**
     * 对象终结时确保关闭上下文
     * Ensure context is shutdown when object is finalized
     *
     * @throws Throwable 如果关闭时发生错误 / If error occurs during shutdown
     */
    public void finalize() throws Throwable {
        if (this.compilationResult != null) {
            log.error("Finalization of initialized ScriptContext. Forcing context shutdown.");
            this.shutdown();
        }

        super.finalize();
    }
}
