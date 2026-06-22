package com.aionemu.commons.services;

import com.aionemu.commons.scripting.scriptmanager.ScriptManager;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javolution.util.FastMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本服务管理类，负责管理游戏中的脚本文件
 * Script service management class, responsible for managing script files in the game
 *
 * 该类提供以下功能:
 * This class provides the following features:
 * 1. 加载单个脚本文件或整个目录的脚本 / Load single script file or scripts from entire directory
 * 2. 卸载脚本 / Unload scripts
 * 3. 重新加载脚本 / Reload scripts
 * 4. 管理脚本管理器实例 / Manage script manager instances
 */
public class ScriptService {
    /** 日志记录器 / Logger instance */
    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);
    
    /** 
     * 存储文件到脚本管理器的映射关系
     * Stores the mapping between files and script managers
     */
    private final Map<File, ScriptManager> map = new FastMap<File, ScriptManager>().shared();

    /**
     * 通过文件路径加载脚本
     * Load script through file path
     *
     * @param file 脚本文件路径 / Script file path
     * @throws RuntimeException 加载失败时抛出 / Thrown when loading fails
     */
    public void load(String file) throws RuntimeException {
        this.load(new File(file));
    }

    /**
     * 加载脚本文件或目录
     * Load script file or directory
     *
     * @param file 脚本文件或目录 / Script file or directory
     * @throws RuntimeException 加载失败时抛出 / Thrown when loading fails
     */
    public void load(File file) throws RuntimeException {
        if (file.isFile()) {
            this.loadFile(file);
        } else if (file.isDirectory()) {
            this.loadDir(file);
        }
    }

    /**
     * 加载单个脚本文件
     * Load single script file
     *
     * @param file 脚本文件 / Script file
     */
    private void loadFile(File file) {
        if (this.map.containsKey(file)) {
            throw new IllegalArgumentException("ScriptManager by file:" + file + " already loaded");
        }
        
        ScriptManager sm = new ScriptManager();
        try {
            sm.load(file);
        } catch (Exception var4) {
            log.error("loadFile", var4);
            throw new RuntimeException(var4);
        }
        
        this.map.put(file, sm);
    }

    /**
     * 加载目录中的所有XML脚本文件
     * Load all XML script files in directory
     *
     * @param dir 脚本目录 / Script directory
     */
    private void loadDir(File dir) {
        Iterator<?> i$ = FileUtils.listFiles(dir, new String[]{"xml"}, false).iterator();
        while (i$.hasNext()) {
            Object file = i$.next();
            this.loadFile((File) file);
        }
    }

    /**
     * 卸载脚本文件
     * Unload script file
     *
     * @param file 要卸载的脚本文件 / Script file to unload
     * @throws IllegalArgumentException 文件未加载时抛出 / Thrown when file is not loaded
     */
    public void unload(File file) throws IllegalArgumentException {
        ScriptManager sm = this.map.remove(file);
        if (sm == null) {
            throw new IllegalArgumentException("ScriptManager by file " + file + " is not loaded.");
        }
        sm.shutdown();
    }

    /**
     * 重新加载脚本文件
     * Reload script file
     *
     * @param file 要重新加载的脚本文件 / Script file to reload
     * @throws IllegalArgumentException 文件未加载时抛出 / Thrown when file is not loaded
     */
    public void reload(File file) throws IllegalArgumentException {
        ScriptManager sm = this.map.get(file);
        if (sm == null) {
            throw new IllegalArgumentException("ScriptManager by file " + file + " is not loaded.");
        }
        sm.reload();
    }

    /**
     * 添加脚本管理器实例
     * Add script manager instance
     *
     * @param scriptManager 脚本管理器实例 / Script manager instance
     * @param file 关联的文件 / Associated file
     */
    public void addScriptManager(ScriptManager scriptManager, File file) {
        if (this.map.containsKey(file)) {
            throw new IllegalArgumentException("ScriptManager by file " + file + " is already loaded.");
        }
        this.map.put(file, scriptManager);
    }

    /**
     * 获取所有已加载的脚本管理器
     * Get all loaded script managers
     *
     * @return 只读的脚本管理器映射 / Unmodifiable map of script managers
     */
    public Map<File, ScriptManager> getLoadedScriptManagers() {
        return Collections.unmodifiableMap(this.map);
    }

    /**
     * 关闭服务，清理所有脚本管理器
     * Shutdown service, cleanup all script managers
     */
    public void shutdown() {
        for (Iterator<Entry<File, ScriptManager>> it = this.map.entrySet().iterator(); it.hasNext(); it.remove()) {
            try {
                it.next().getValue().shutdown();
            } catch (Exception var3) {
                log.warn("An exception occurred during shutdown procedure.", var3);
            }
        }
    }
}
