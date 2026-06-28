package com.aionemu.commons.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * 类工具类，提供了类的继承关系判断、包成员判断和类名获取等功能
 * Class utility for checking inheritance relationships, package membership and class name retrieval
 */
@Slf4j
public class ClassUtils {

    /**
     * 判断类a是否是类b的子类或实现了接口b
     * Check if class a is a subclass of or implements interface b
     * 
     * @param a 要检查的类 / Class to check
     * @param b 目标父类或接口 / Target superclass or interface
     * @return 如果a是b的子类或实现了接口b则返回true / Returns true if a is subclass of or implements b
     */
    public static boolean isSubclass(Class<?> a, Class<?> b) {
        if (a == b) {
            return true;
        } else if (a != null && b != null) {
            for (Class<?> x = a; x != null; x = x.getSuperclass()) {
                if (x == b) {
                    return true;
                }
                if (b.isInterface()) {
                    for (Class<?> anInterface : x.getInterfaces()) {
                        if (isSubclass(anInterface, b)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * 判断类是否属于指定包
     * Check if class belongs to specified package
     * 
     * @param clazz 要检查的类 / Class to check
     * @param packageName 包名 / Package name
     * @return 如果类属于指定包则返回true / Returns true if class belongs to package
     */
    public static boolean isPackageMember(Class<?> clazz, String packageName) {
        return isPackageMember(clazz.getName(), packageName);
    }

    /**
     * 判断类名是否属于指定包
     * Check if class name belongs to specified package
     * 
     * @param className 类名 / Class name
     * @param packageName 包名 / Package name
     * @return 如果类名属于指定包则返回true / Returns true if class name belongs to package
     */
    public static boolean isPackageMember(String className, String packageName) {
        if (className.contains(".")) {
            String classPackage = className.substring(0, className.lastIndexOf('.'));
            return packageName.equals(classPackage);
        } else {
            return packageName == null || packageName.isEmpty();
        }
    }

    /**
     * 从目录中获取所有类名
     * Get all class names from directory
     * 
     * @param directory 目录 / Directory
     * @return 类名集合 / Set of class names
     * @throws IllegalArgumentException 如果目录不存在或不是目录 / If directory doesn't exist or is not a directory
     */
    public static Set<String> getClassNamesFromDirectory(File directory) throws IllegalArgumentException {
        if (directory.isDirectory() && directory.exists()) {
            return getClassNamesFromPackage(directory, null, true);
        } else {
            throw new IllegalArgumentException("Directory " + directory + " doesn't exists or is not directory");
        }
    }

    /**
     * 从包中获取所有类名
     * Get all class names from package
     * 
     * @param directory 目录 / Directory
     * @param packageName 包名 / Package name
     * @param recursive 是否递归搜索子目录 / Whether to search subdirectories recursively
     * @return 类名集合 / Set of class names
     */
    public static Set<String> getClassNamesFromPackage(File directory, String packageName, boolean recursive) {
        Set<String> classes = new HashSet<String>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    String newPackage = file.getName();
                    if (!GenericValidator.isBlankOrNull(packageName)) {
                        newPackage = packageName + "." + newPackage;
                    }
                    classes.addAll(getClassNamesFromPackage(file, newPackage, recursive));
                }
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                if (!GenericValidator.isBlankOrNull(packageName)) {
                    className = packageName + "." + className;
                }
                classes.add(className);
            }
        }
        return classes;
    }

    /**
     * 从JAR文件中获取所有类名
     * Get all class names from JAR file
     * 
     * @param file JAR文件 / JAR file
     * @return 类名集合 / Set of class names
     * @throws IOException 如果读取JAR文件失败 / If failed to read JAR file
     */
    public static Set<String> getClassNamesFromJarFile(File file) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            Set<String> nestedJarClasses = getClassNamesFromNestedJarFile(file);
            if (nestedJarClasses != null) {
                return nestedJarClasses;
            }
            throw new IllegalArgumentException("File " + file + " is not valid jar file");
        }

        Set<String> result = new HashSet<String>();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6);
                    name = name.replace('/', '.');
                    result.add(name);
                }
            }
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    log.error("Failed to close jar file " + jarFile.getName(), e);
                }
            }
        }
        return result;
    }

    private static Set<String> getClassNamesFromNestedJarFile(File file) throws IOException {
        String path = file.getPath().replace('\\', '/');
        int separator = path.indexOf(".jar/!");
        if (separator < 0) {
            return null;
        }

        File outerJar = new File(path.substring(0, separator + ".jar".length()));
        String nestedEntryName = path.substring(separator + ".jar/!".length());
        if (!outerJar.exists() || outerJar.isDirectory() || nestedEntryName.isEmpty()) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        JarFile jarFile = null;
        JarInputStream nestedJar = null;
        try {
            jarFile = new JarFile(outerJar);
            JarEntry nestedEntry = jarFile.getJarEntry(nestedEntryName);
            if (nestedEntry == null) {
                return null;
            }

            InputStream inputStream = jarFile.getInputStream(nestedEntry);
            nestedJar = new JarInputStream(inputStream);
            JarEntry entry;
            while ((entry = nestedJar.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6);
                    name = name.replace('/', '.');
                    result.add(name);
                }
            }
        } finally {
            if (nestedJar != null) {
                try {
                    nestedJar.close();
                } catch (IOException e) {
                    log.error("Failed to close nested jar stream " + file, e);
                }
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    log.error("Failed to close jar file " + jarFile.getName(), e);
                }
            }
        }
        return result;
    }
}
