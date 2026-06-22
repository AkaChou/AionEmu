package com.aionemu.commons.scripting.scriptmanager;

import com.aionemu.commons.scripting.ScriptCompiler;
import com.aionemu.commons.scripting.ScriptContext;
import com.aionemu.commons.scripting.ScriptContextFactory;
import com.aionemu.commons.scripting.classlistener.ClassListener;
import com.aionemu.commons.scripting.impl.javacompiler.ScriptCompilerImpl;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本管理器类，负责脚本的加载、卸载和重新加载
 * Script manager class, responsible for loading, unloading and reloading scripts
 *
 * 该类提供以下主要功能：
 * This class provides the following main features:
 * 1. 从XML文件加载脚本配置 / Load script configuration from XML file
 * 2. 从目录加载脚本 / Load scripts from directory
 * 3. 管理脚本上下文 / Manage script contexts
 * 4. 支持脚本热重载 / Support script hot reloading
 *
 * @author SunAion Team
 */
public class ScriptManager {
    /** 
     * 日志记录器 / Logger instance 
     */
    private static final Logger log = LoggerFactory.getLogger(ScriptManager.class);
    
    /** 
     * 默认脚本编译器类 / Default script compiler class 
     */
    public static final Class<? extends ScriptCompiler> DEFAULT_COMPILER_CLASS = ScriptCompilerImpl.class;
    
    /** 
     * 脚本上下文集合 / Collection of script contexts 
     */
    private Set<ScriptContext> contexts = new HashSet();
    
    /** 
     * 全局类监听器 / Global class listener 
     */
    private ClassListener globalClassListener;

    /**
     * 从XML文件加载脚本配置
     * Load script configuration from XML file
     * @param scriptDescriptor XML配置文件 / XML configuration file
     * @throws Exception 加载失败时抛出 / Thrown when loading fails
     */
    public synchronized void load(File scriptDescriptor) throws Exception {
        FileInputStream fin = new FileInputStream(scriptDescriptor);
        JAXBContext c = JAXBContext.newInstance(ScriptInfo.class, ScriptList.class);
        Unmarshaller u = c.createUnmarshaller();
        ScriptList list = null;

        try {
            list = (ScriptList)u.unmarshal(fin);
        } catch (Exception var11) {
            throw var11;
        } finally {
            if (fin != null) {
                fin.close();
            }
        }

        Iterator i$ = list.getScriptInfos().iterator();
        while(i$.hasNext()) {
            ScriptInfo si = (ScriptInfo)i$.next();
            resolveScriptInfoPaths(scriptDescriptor, si);
            ScriptContext context = this.createContext(si, null);
            if (context != null) {
                this.contexts.add(context);
                context.init();
            }
        }
    }

    private void resolveScriptInfoPaths(File scriptDescriptor, ScriptInfo scriptInfo) {
        scriptInfo.setRoot(resolveScriptPath(scriptDescriptor, scriptInfo.getRoot()));
        if (scriptInfo.getLibraries() != null) {
            List<File> libraries = new ArrayList();
            for (File library : scriptInfo.getLibraries()) {
                libraries.add(resolveScriptPath(scriptDescriptor, library));
            }
            scriptInfo.setLibraries(libraries);
        }
        if (scriptInfo.getScriptInfos() != null) {
            for (ScriptInfo child : scriptInfo.getScriptInfos()) {
                resolveScriptInfoPaths(scriptDescriptor, child);
            }
        }
    }

    private File resolveScriptPath(File scriptDescriptor, File path) {
        if (path == null || path.isAbsolute() || path.exists()) {
            return path;
        }

        File moduleRoot = findModuleRoot(scriptDescriptor);
        if (moduleRoot != null) {
            File moduleRelative = new File(moduleRoot, path.getPath());
            if (moduleRelative.exists()) {
                return moduleRelative;
            }
        }

        File descriptorParent = scriptDescriptor.getAbsoluteFile().getParentFile();
        if (descriptorParent != null) {
            return new File(descriptorParent, path.getPath());
        }
        return path;
    }

    private File findModuleRoot(File scriptDescriptor) {
        File current = scriptDescriptor.getAbsoluteFile().getParentFile();
        while (current != null) {
            if ("data".equals(current.getName())) {
                return current.getParentFile();
            }
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * 从目录加载脚本，自动搜索JAR文件
     * Load scripts from directory, automatically search for JAR files
     * @param directory 脚本目录 / Script directory
     * @throws RuntimeException 加载失败时抛出 / Thrown when loading fails
     */
    public synchronized void loadDirectory(File directory) throws RuntimeException {
        Collection<File> libraries = FileUtils.listFiles(directory, new String[]{"jar"}, true);
        ArrayList list = Lists.newArrayList(libraries);

        try {
            this.loadDirectory(directory, list, DEFAULT_COMPILER_CLASS.getName());
        } catch (Exception var5) {
            throw new RuntimeException("Failed to load script context from directory " + directory.getAbsolutePath(), var5);
        }
    }

    /**
     * 从目录加载脚本，使用指定的库文件和编译器
     * Load scripts from directory with specified libraries and compiler
     * @param directory 脚本目录 / Script directory
     * @param libraries 库文件列表 / Library file list
     * @param compilerClassName 编译器类名 / Compiler class name
     * @throws Exception 加载失败时抛出 / Thrown when loading fails
     */
    public synchronized void loadDirectory(File directory, List<File> libraries, String compilerClassName) throws Exception {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("File should be directory");
        }
        
        ScriptInfo si = new ScriptInfo();
        si.setRoot(directory);
        si.setCompilerClass(compilerClassName);
        si.setScriptInfos(Collections.emptyList());
        si.setLibraries(libraries);
        ScriptContext sc = this.createContext(si, null);
        this.contexts.add(sc);
        sc.init();
    }

    /**
     * 创建脚本上下文
     * Create script context
     * @param si 脚本信息 / Script information
     * @param parent 父上下文 / Parent context
     * @return 创建的上下文 / Created context
     * @throws Exception 创建失败时抛出 / Thrown when creation fails
     */
    protected ScriptContext createContext(ScriptInfo si, ScriptContext parent) throws Exception {
        ScriptContext context = ScriptContextFactory.getScriptContext(si.getRoot(), parent);
        context.setLibraries(si.getLibraries());
        context.setCompilerClassName(si.getCompilerClass());
        
        if (parent == null && this.contexts.contains(context)) {
            log.warn("Double root script context definition: " + si.getRoot().getAbsolutePath());
            return null;
        }
        
        if (si.getScriptInfos() != null && !si.getScriptInfos().isEmpty()) {
            for (ScriptInfo child : si.getScriptInfos()) {
                this.createContext(child, context);
            }
        }

        if (parent == null && this.globalClassListener != null) {
            context.setClassListener(this.globalClassListener);
        }

        return context;
    }

    /**
     * 关闭所有脚本上下文
     * Shutdown all script contexts
     */
    public synchronized void shutdown() {
        for (ScriptContext context : this.contexts) {
            context.shutdown();
        }
        this.contexts.clear();
    }

    /**
     * 重新加载所有脚本上下文
     * Reload all script contexts
     */
    public synchronized void reload() {
        for (ScriptContext context : this.contexts) {
            this.reloadContext(context);
        }
    }

    /**
     * 重新加载指定的脚本上下文
     * Reload specified script context
     * @param ctx 要重新加载的上下文 / Context to reload
     */
    public void reloadContext(ScriptContext ctx) {
        ctx.reload();
    }

    /**
     * 获取所有脚本上下文
     * Get all script contexts
     * @return 只读的上下文集合 / Unmodifiable collection of contexts
     */
    public synchronized Collection<ScriptContext> getScriptContexts() {
        return Collections.unmodifiableSet(this.contexts);
    }

    /**
     * 设置全局类监听器
     * Set global class listener
     * @param instance 监听器实例 / Listener instance
     */
    public void setGlobalClassListener(ClassListener instance) {
        this.globalClassListener = instance;
    }
}
