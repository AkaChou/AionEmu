package com.aionemu.commons.scripting;

import com.aionemu.commons.scripting.url.VirtualClassURLStreamHandler;
import com.aionemu.commons.utils.ClassUtils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本类加载器，用于加载和管理编译后的脚本类
 * Script class loader for loading and managing compiled script classes
 *
 * 继承自URLClassLoader，提供了对编译后脚本类的加载和管理功能
 * Extends URLClassLoader to provide loading and management of compiled script classes
 *
 * @author SunAion Team
 */
public abstract class ScriptClassLoader extends URLClassLoader {
    /** 日志记录器 / Logger instance */
    private static final Logger log = LoggerFactory.getLogger(ScriptClassLoader.class);
    
    /** 虚拟类URL流处理器 / Virtual class URL stream handler */
    private final VirtualClassURLStreamHandler urlStreamHandler = new VirtualClassURLStreamHandler(this);
    
    /** 库类名称集合 / Set of library class names */
    private Set<String> libraryClassNames = new HashSet();
    
    /** 已加载库文件集合 / Set of loaded library files */
    private Set<File> loadedLibraries = new HashSet();

    /**
     * 构造函数，使用指定的URL数组和父类加载器初始化
     * Constructor with URLs and parent class loader
     *
     * @param urls URL数组，指定类加载的位置 / Array of URLs specifying locations to load classes from
     * @param parent 父类加载器 / Parent class loader
     */
    public ScriptClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * 构造函数，仅使用URL数组初始化
     * Constructor with only URLs
     *
     * @param urls URL数组，指定类加载的位置 / Array of URLs specifying locations to load classes from
     */
    public ScriptClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * 构造函数，使用URL数组、父类加载器和URL流处理工厂初始化
     * Constructor with URLs, parent class loader and URL stream handler factory
     *
     * @param urls URL数组 / Array of URLs
     * @param parent 父类加载器 / Parent class loader
     * @param factory URL流处理工厂 / URL stream handler factory
     */
    public ScriptClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    /**
     * 添加JAR文件到类加载器
     * Add a JAR file to the class loader
     *
     * @param file JAR文件 / JAR file to add
     * @throws IOException 如果读取JAR文件失败 / If reading JAR file fails
     */
    public void addJarFile(File file) throws IOException {
        if (!this.loadedLibraries.contains(file)) {
            Set<String> jarFileClasses = ClassUtils.getClassNamesFromJarFile(file);
            this.libraryClassNames.addAll(jarFileClasses);
            this.loadedLibraries.add(file);
        }
    }

    /**
     * 获取资源URL
     * Get resource URL
     *
     * @param name 资源名称 / Resource name
     * @return 资源URL / Resource URL
     */
    public URL getResource(String name) {
        if (!name.endsWith(".class")) {
            return super.getResource(name);
        } else {
            String newName = name.substring(0, name.length() - 6);
            newName = newName.replace('/', '.');
            if (this.getCompiledClasses().contains(newName)) {
                try {
                    return new URL((URL)null, "aescript://" + newName, this.urlStreamHandler);
                } catch (MalformedURLException var4) {
                    log.error("Can't create url for compiled class", var4);
                }
            }
            return super.getResource(name);
        }
    }

    /**
     * 加载指定名称的类
     * Load class with specified name
     *
     * @param name 类名 / Class name
     * @return 加载的类 / Loaded class
     * @throws ClassNotFoundException 如果找不到类 / If class not found
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        boolean isCompiled = this.getCompiledClasses().contains(name);
        if (!isCompiled) {
            return super.loadClass(name, true);
        } else {
            Class<?> c = this.getDefinedClass(name);
            if (c == null) {
                byte[] b = this.getByteCode(name);
                c = super.defineClass(name, b, 0, b.length);
                this.setDefinedClass(name, c);
            }
            return c;
        }
    }

    /**
     * 获取库类名称集合
     * Get set of library class names
     *
     * @return 不可修改的库类名称集合 / Unmodifiable set of library class names
     */
    protected Set<String> getLibraryClassNames() {
        return Collections.unmodifiableSet(this.libraryClassNames);
    }

    /**
     * 获取已编译的类集合
     * Get set of compiled classes
     *
     * @return 已编译的类名称集合 / Set of compiled class names
     */
    public abstract Set<String> getCompiledClasses();

    /**
     * 获取类的字节码
     * Get bytecode of a class
     *
     * @param className 类名 / Class name
     * @return 类的字节码 / Class bytecode
     */
    public abstract byte[] getByteCode(String className);

    /**
     * 获取已定义的类
     * Get defined class
     *
     * @param className 类名 / Class name
     * @return 已定义的类 / Defined class
     */
    public abstract Class<?> getDefinedClass(String className);

    /**
     * 设置已定义的类
     * Set defined class
     *
     * @param className 类名 / Class name
     * @param clazz 类对象 / Class object
     * @throws IllegalArgumentException 如果参数无效 / If arguments are invalid
     */
    public abstract void setDefinedClass(String className, Class<?> clazz) throws IllegalArgumentException;
}
