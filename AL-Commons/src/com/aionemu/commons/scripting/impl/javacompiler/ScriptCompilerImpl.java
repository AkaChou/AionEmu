package com.aionemu.commons.scripting.impl.javacompiler;

import com.aionemu.commons.scripting.CompilationResult;
import com.aionemu.commons.scripting.ScriptClassLoader;
import com.aionemu.commons.scripting.ScriptCompiler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本编译器实现类，负责Java源代码的编译
 * Script compiler implementation class, responsible for compiling Java source code
 *
 * 该类实现了ScriptCompiler接口，提供以下功能：
 * 1. 支持单个或多个Java类的编译
 * 2. 管理编译过程的类加载器层次结构
 * 3. 处理编译时的库依赖
 * 4. 提供编译结果的封装
 *
 * This class implements the ScriptCompiler interface and provides:
 * 1. Support for compiling single or multiple Java classes
 * 2. Management of class loader hierarchy during compilation
 * 3. Handling of library dependencies during compilation
 * 4. Encapsulation of compilation results
 */
public class ScriptCompilerImpl implements ScriptCompiler {
    
    /**
     * 日志记录器实例
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ScriptCompilerImpl.class);
    
    /**
     * Java编译器实例
     * Java compiler instance
     */
    protected final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    
    /**
     * 编译时需要的库文件集合
     * Collection of library files needed for compilation
     */
    protected Iterable<File> libraries;
    
    /**
     * 父级类加载器
     * Parent class loader
     */
    protected ScriptClassLoader parentClassLoader;

    /**
     * 构造函数，初始化编译器并检查可用性
     * Constructor, initializes the compiler and checks availability
     *
     * @throws RuntimeException 如果编译器不可用 / If compiler is not available
     */
    public ScriptCompilerImpl() {
        if (this.javaCompiler == null) {
            throw new RuntimeException(new InstantiationException("JavaCompiler is not available."));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentClassLoader(ScriptClassLoader classLoader) {
        this.parentClassLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLibraires(Iterable<File> files) {
        this.libraries = files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompilationResult compile(String className, String sourceCode) {
        return this.compile(new String[]{className}, new String[]{sourceCode});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompilationResult compile(String[] classNames, String[] sourceCode) throws IllegalArgumentException {
        if (classNames.length != sourceCode.length) {
            throw new IllegalArgumentException("Amount of classes is not equal to amount of sources");
        }
        
        List<JavaFileObject> compilationUnits = new ArrayList<>();
        for (int i = 0; i < classNames.length; ++i) {
            JavaFileObject compilationUnit = new JavaSourceFromString(classNames[i], sourceCode[i]);
            compilationUnits.add(compilationUnit);
        }
        
        return this.doCompilation(compilationUnits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompilationResult compile(Iterable<File> compilationUnits) {
        List<JavaFileObject> list = new ArrayList<>();
        for (File f : compilationUnits) {
            list.add(new JavaSourceFromFile(f, Kind.SOURCE));
        }
        
        return this.doCompilation(list);
    }

    /**
     * 执行实际的编译过程
     * Performs the actual compilation process
     *
     * @param compilationUnits 要编译的编译单元集合 / Collection of compilation units to compile
     * @return 编译结果 / Compilation result
     * @throws RuntimeException 如果编译失败 / If compilation fails
     */
    protected CompilationResult doCompilation(Iterable<JavaFileObject> compilationUnits) {
        List<String> options = Arrays.asList("-encoding", "UTF-8", "-g");
        DiagnosticListener<JavaFileObject> listener = new ErrorListener();
        ClassFileManager manager = new ClassFileManager(this.javaCompiler, listener);
        manager.setParentClassLoader(this.parentClassLoader);
        
        if (this.libraries != null) {
            try {
                manager.addLibraries(this.libraries);
            } catch (IOException var8) {
                log.error("Can't set libraries for compiler.", var8);
            }
        }

        CompilationTask task = this.javaCompiler.getTask(null, manager, listener, options, null, compilationUnits);
        if (!task.call()) {
            throw new RuntimeException("Error while compiling classes");
        }
        
        ScriptClassLoader cl = manager.getClassLoader(null);
        Class<?>[] compiledClasses = this.classNamesToClasses(manager.getCompiledClasses().keySet(), cl);
        return new CompilationResult(compiledClasses, cl);
    }

    /**
     * 将类名集合转换为对应的Class对象数组
     * Converts a collection of class names to corresponding Class objects
     *
     * @param classNames 类名集合 / Collection of class names
     * @param cl 用于加载类的类加载器 / Class loader for loading classes
     * @return 类对象数组 / Array of Class objects
     * @throws RuntimeException 如果类加载失败 / If class loading fails
     */
    protected Class<?>[] classNamesToClasses(Collection<String> classNames, ScriptClassLoader cl) {
        Class<?>[] classes = new Class[classNames.size()];
        int i = 0;
        
        for (String className : classNames) {
            try {
                Class<?> clazz = cl.loadClass(className);
                classes[i++] = clazz;
            } catch (ClassNotFoundException var8) {
                throw new RuntimeException(var8);
            }
        }
        
        return classes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getSupportedFileTypes() {
        return new String[]{"java"};
    }
}
