package com.aionemu.commons.utils;

import com.aionemu.boot.i18n.I18n;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 类继承关系、包归属与类名扫描工具。
 * Class inheritance, package membership and class-name scanning helpers.
 */
@Slf4j
@UtilityClass
public class ClassUtils {

    /**
     * 判断类 {@code a} 是否为 {@code b} 的子类或实现了接口 {@code b}。
     * Whether class {@code a} is a subclass of or implements {@code b}.
     *
     * @param a 待检查类 / Class to check
     * @param b 目标父类或接口 / Target superclass or interface
     * @return 若 subclass or implementor 则为 true / True if subclass or implementor
     */
    public boolean isSubclass(Class<?> a, Class<?> b) {
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
     * 判断类是否属于指定包。
     * Whether the class belongs to the given package.
     *
     * @param clazz       待检查类 / Class to check
     * Package name
     *
     * @return 若 package member 则为 true / True if package member
     */
    public boolean isPackageMember(Class<?> clazz, String packageName) {
        return isPackageMember(clazz.getName(), packageName);
    }

    /**
     * 判断类名是否属于指定包。
     * Whether the class name belongs to the given package.
     *
     * Class name
     * Package name
     *
     * @return 若 package member 则为 true / True if package member
     */
    public boolean isPackageMember(String className, String packageName) {
        if (className.contains(".")) {
            String classPackage = className.substring(0, className.lastIndexOf('.'));
            return packageName.equals(classPackage);
        } else {
            return packageName == null || packageName.isEmpty();
        }
    }

    /**
     * 从目录扫描全部类名。
     * Collect all class names under a directory.
     *
     * Directory
     * Set of class names
     *
     * @param directory @throws IllegalArgumentException 目录无效时 / When directory is invalid
     */
    public Set<String> getClassNamesFromDirectory(File directory) throws IllegalArgumentException {
        if (directory.isDirectory() && directory.exists()) {
            return getClassNamesFromPackage(directory, null, true);
        } else {
            throw new IllegalArgumentException("Directory " + directory + " doesn't exists or is not directory");
        }
    }

    /**
     * 从包目录扫描类名。
     * Collect class names from a package directory.
     *
     * Directory
     * Package name
     * @param recursive   是否递归子目录 / Whether to recurse
     * Set of class names
     */
    public Set<String> getClassNamesFromPackage(File directory, String packageName, boolean recursive) {
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
     * 从 JAR（含嵌套 JAR 路径）读取类名。
     * Collect class names from a JAR (including nested JAR paths).
     *
     * JAR file
     * Set of class names
     * On read failure
     */
    public Set<String> getClassNamesFromJarFile(File file) throws IOException {
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
                    log.error(I18n.get("log.5f2c41da34b9", jarFile.getName(), e));
                }
            }
        }
        return result;
    }

    /**
     * 从嵌套 JAR 路径（{@code outer.jar/!entry.jar}）读取类名。
     * Collect class names from a nested JAR path ({@code outer.jar/!entry.jar}).
     *
     * @param file 嵌套路径文件对象 / Nested path file object
     * @return 类名集合；无法解析时返回 null / Class names, or null if unresolvable
     * On read failure
     */
    private Set<String> getClassNamesFromNestedJarFile(File file) throws IOException {
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
                    log.error(I18n.get("log.aad47c1b2c2d", file, e));
                }
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    log.error(I18n.get("log.5f2c41da34b9", jarFile.getName(), e));
                }
            }
        }
        return result;
    }
}
