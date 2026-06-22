package com.aionemu.commons.scripting.impl.javacompiler;

import com.aionemu.commons.scripting.ScriptClassLoader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;

/**
 * 类文件管理器，负责管理编译后的类文件和类加载
 * Class File Manager for managing compiled class files and class loading
 *
 * 该类继承自ForwardingJavaFileManager，主要功能包括：
 * 1. 管理编译后的类文件
 * 2. 维护类加载器层次结构
 * 3. 处理类文件的输入输出
 * 4. 管理类路径和库依赖
 *
 * This class extends ForwardingJavaFileManager and provides:
 * 1. Management of compiled class files
 * 2. Maintenance of class loader hierarchy
 * 3. Handling of class file I/O
 * 4. Management of classpath and library dependencies
 */
public class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    
    /**
     * 存储编译后的类文件映射表
     * Map to store compiled class files
     */
    private final Map<String, BinaryClass> compiledClasses = new HashMap<>();
    
    /**
     * 脚本类加载器实例
     * Script class loader instance
     */
    protected ScriptClassLoaderImpl loader;
    
    /**
     * 父级类加载器
     * Parent class loader
     */
    protected ScriptClassLoader parentClassLoader;

    /**
     * 构造函数，使用指定的编译器和诊断监听器初始化
     * Constructor, initializes with specified compiler and diagnostic listener
     *
     * @param compiler 编译器实例 / Compiler instance
     * @param listener 诊断监听器 / Diagnostic listener
     */
    public ClassFileManager(JavaCompiler compiler, DiagnosticListener<? super JavaFileObject> listener) {
        super(compiler.getStandardFileManager(listener, null, null));
    }

    /**
     * 获取Java文件输出对象
     * Get Java file output object
     *
     * @param location 位置 / Location
     * @param className 类名 / Class name
     * @param kind 文件类型 / File kind
     * @param sibling 相关文件对象 / Sibling file object
     * @return Java文件对象 / Java file object
     * @throws IOException 如果创建文件对象失败 / If failed to create file object
     */
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
        BinaryClass co = new BinaryClass(className);
        this.compiledClasses.put(className, co);
        return co;
    }

    /**
     * 获取类加载器实例
     * Get class loader instance
     *
     * @param location 位置 / Location
     * @return 脚本类加载器实例 / Script class loader instance
     */
    public synchronized ScriptClassLoaderImpl getClassLoader(Location location) {
        if (this.loader == null) {
            if (this.parentClassLoader != null) {
                this.loader = new ScriptClassLoaderImpl(this, this.parentClassLoader);
            } else {
                this.loader = new ScriptClassLoaderImpl(this);
            }
        }
        return this.loader;
    }

    /**
     * 设置父级类加载器
     * Set parent class loader
     *
     * @param classLoader 父级类加载器 / Parent class loader
     */
    public void setParentClassLoader(ScriptClassLoader classLoader) {
        this.parentClassLoader = classLoader;
    }

    /**
     * 添加单个库文件
     * Add a single library file
     *
     * @param file 库文件 / Library file
     * @throws IOException 如果添加库文件失败 / If failed to add library file
     */
    public void addLibrary(File file) throws IOException {
        ScriptClassLoaderImpl classLoader = this.getClassLoader(null);
        classLoader.addJarFile(file);
    }

    /**
     * 添加多个库文件
     * Add multiple library files
     *
     * @param files 库文件集合 / Collection of library files
     * @throws IOException 如果添加库文件失败 / If failed to add library files
     */
    public void addLibraries(Iterable<File> files) throws IOException {
        for (File f : files) {
            this.addLibrary(f);
        }
    }

    /**
     * 获取编译后的类文件映射表
     * Get map of compiled class files
     *
     * @return 编译后的类文件映射表 / Map of compiled class files
     */
    public Map<String, BinaryClass> getCompiledClasses() {
        return this.compiledClasses;
    }

    /**
     * 列出指定位置和包名下的Java文件对象
     * List Java file objects for specified location and package
     *
     * @param location 位置 / Location
     * @param packageName 包名 / Package name
     * @param kinds 文件类型集合 / Set of file kinds
     * @param recurse 是否递归查找 / Whether to search recursively
     * @return Java文件对象集合 / Collection of Java file objects
     * @throws IOException 如果列出文件失败 / If failed to list files
     */
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        Iterable<JavaFileObject> objects = super.list(location, packageName, kinds, recurse);
        if (StandardLocation.CLASS_PATH.equals(location) && kinds.contains(Kind.CLASS)) {
            List<JavaFileObject> temp = new ArrayList<>();
            for (JavaFileObject object : objects) {
                temp.add(object);
            }
            temp.addAll(this.loader.getClassesForPackage(packageName));
            objects = temp;
        }
        return objects;
    }

    /**
     * 推断二进制名称
     * Infer binary name
     *
     * @param location 位置 / Location
     * @param file Java文件对象 / Java file object
     * @return 二进制名称 / Binary name
     */
    public String inferBinaryName(Location location, JavaFileObject file) {
        return file instanceof BinaryClass ? ((BinaryClass)file).inferBinaryName(null) : super.inferBinaryName(location, file);
    }
}
