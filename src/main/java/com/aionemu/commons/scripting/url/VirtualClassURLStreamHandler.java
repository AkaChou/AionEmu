package com.aionemu.commons.scripting.url;

import com.aionemu.commons.scripting.ScriptClassLoader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * 虚拟类URL流处理器，用于处理编译后的脚本类的URL流
 * Virtual class URL stream handler for handling URL streams of compiled script classes
 *
 * 继承自URLStreamHandler，提供了对编译后脚本类URL连接的创建功能
 * Extends URLStreamHandler to provide creation of URL connections for compiled script classes
 *
 * @author SunAion Team
 */
public class VirtualClassURLStreamHandler extends URLStreamHandler {
    /** 处理器协议前缀 / Handler protocol prefix */
    public static final String HANDLER_PROTOCOL = "aescript://";
    
    /** 脚本类加载器 / Script class loader */
    private final ScriptClassLoader cl;

    /**
     * 构造函数，使用脚本类加载器初始化处理器
     * Constructor to initialize handler with script class loader
     *
     * @param cl 脚本类加载器 / Script class loader
     */
    public VirtualClassURLStreamHandler(ScriptClassLoader cl) {
        this.cl = cl;
    }

    /**
     * 打开URL连接
     * Open URL connection
     *
     * @param u URL对象 / URL object
     * @return URL连接对象 / URL connection object
     * @throws IOException 如果创建连接失败 / If creating connection fails
     */
    protected URLConnection openConnection(URL u) throws IOException {
        return new VirtualClassURLConnection(u, this.cl);
    }
}
