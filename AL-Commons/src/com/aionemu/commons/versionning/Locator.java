package com.aionemu.commons.versionning;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import javax.tools.ToolProvider;

/**
 * 资源定位器类，用于定位类文件和资源的物理位置
 * Resource locator class for locating physical locations of class files and resources
 */
public final class Locator {
    /**
     * 私有构造函数，防止实例化
     * Private constructor to prevent instantiation
     */
    private Locator() {
    }

    /**
     * 获取指定类的源文件位置
     * Get the source file location of the specified class
     *
     * @param c 要定位的类 The class to locate
     * @return 类文件的物理位置 Physical location of the class file
     */
    public static File getClassSource(Class<?> c) {
        String classResource = c.getName().replace('.', '/') + ".class";
        return getResourceSource(c.getClassLoader(), classResource);
    }

    /**
     * 获取指定资源的源文件位置
     * Get the source file location of the specified resource
     *
     * @param c 类加载器 Class loader
     * @param resource 资源路径 Resource path
     * @return 资源文件的物理位置 Physical location of the resource file
     */
    public static File getResourceSource(ClassLoader c, String resource) {
        if (c == null) {
            c = Locator.class.getClassLoader();
        }

        URL url = null;
        if (c == null) {
            url = ClassLoader.getSystemResource(resource);
        } else {
            url = c.getResource(resource);
        }

        if (url != null) {
            String u = url.toString();
            int tail;
            String dirName;
            if (u.startsWith("jar:file:")) {
                tail = u.indexOf("!");
                dirName = u.substring(4, tail);
                return new File(fromURI(dirName));
            }

            if (u.startsWith("file:")) {
                tail = u.indexOf(resource);
                dirName = u.substring(0, tail);
                return new File(fromURI(dirName));
            }
        }

        return null;
    }

    /**
     * 将URI转换为文件系统路径
     * Convert URI to file system path
     *
     * @param uri URI字符串 URI string
     * @return 文件系统路径 File system path
     * @throws IllegalArgumentException 如果URI不是有效的文件URI If URI is not a valid file URI
     */
    public static String fromURI(String uri) {
        URL url = null;

        try {
            url = new URL(uri);
        } catch (MalformedURLException var6) {
        }

        if (url != null && "file".equals(url.getProtocol())) {
            StringBuffer buf = new StringBuffer(url.getHost());
            if (buf.length() > 0) {
                buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
            }

            String file = url.getFile();
            int queryPos = file.indexOf(63);
            buf.append(queryPos < 0 ? file : file.substring(0, queryPos));
            uri = buf.toString().replace('/', File.separatorChar);
            if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2 
                && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(58) > -1) {
                uri = uri.substring(1);
            }

            String path = decodeUri(uri);
            return path;
        } else {
            throw new IllegalArgumentException("Can only handle valid file: URIs");
        }
    }

    /**
     * 解码URI字符串中的百分号编码
     * Decode percent encoding in URI string
     *
     * @param uri 要解码的URI字符串 URI string to decode
     * @return 解码后的字符串 Decoded string
     */
    private static String decodeUri(String uri) {
        if (uri.indexOf(37) == -1) {
            return uri;
        } else {
            StringBuffer sb = new StringBuffer();
            CharacterIterator iter = new StringCharacterIterator(uri);

            for(char c = iter.first(); c != '\uffff'; c = iter.next()) {
                if (c == '%') {
                    char c1 = iter.next();
                    if (c1 != '\uffff') {
                        int i1 = Character.digit(c1, 16);
                        char c2 = iter.next();
                        if (c2 != '\uffff') {
                            int i2 = Character.digit(c2, 16);
                            sb.append((char)((i1 << 4) + i2));
                        }
                    }
                } else {
                    sb.append(c);
                }
            }

            String path = sb.toString();
            return path;
        }
    }

    /**
     * 获取系统Java编译器的位置
     * Get the location of the system Java compiler.
     *
     * @return JDK 9+ exposes the compiler through the runtime image; returns null when it is available.
     */
    public static File getToolsJar() {
        if (ToolProvider.getSystemJavaCompiler() == null) {
            System.out.println("Unable to locate the system Java compiler. Run this application with a JDK, not a JRE.");
        }
        return null;
    }

    /**
     * 获取指定位置的JAR文件URL数组
     * Get array of URLs for JAR files at specified location
     *
     * @param location 要搜索的位置 Location to search
     * @return JAR文件URL数组 Array of JAR file URLs
     * @throws MalformedURLException 如果URL格式错误 If URL format is invalid
     */
    public static URL[] getLocationURLs(File location) throws MalformedURLException {
        return getLocationURLs(location, new String[]{".jar"});
    }

    /**
     * 获取指定位置的指定扩展名文件的URL数组
     * Get array of URLs for files with specified extensions at specified location
     *
     * @param location 要搜索的位置 Location to search
     * @param extensions 文件扩展名数组 Array of file extensions
     * @return 文件URL数组 Array of file URLs
     * @throws MalformedURLException 如果URL格式错误 If URL format is invalid
     */
    public static URL[] getLocationURLs(File location, final String[] extensions) throws MalformedURLException {
        URL[] urls = new URL[0];
        if (!location.exists()) {
            return urls;
        } else {
            int i;
            if (!location.isDirectory()) {
                urls = new URL[1];
                String path = location.getPath();

                for(i = 0; i < extensions.length; ++i) {
                    if (path.toLowerCase().endsWith(extensions[i])) {
                        urls[0] = location.toURI().toURL();
                        break;
                    }
                }

                return urls;
            } else {
                File[] matches = location.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        for(int i = 0; i < extensions.length; ++i) {
                            if (name.toLowerCase().endsWith(extensions[i])) {
                                return true;
                            }
                        }

                        return false;
                    }
                });
                urls = new URL[matches.length];

                for(i = 0; i < matches.length; ++i) {
                    urls[i] = matches[i].toURI().toURL();
                }

                return urls;
            }
        }
    }
}
