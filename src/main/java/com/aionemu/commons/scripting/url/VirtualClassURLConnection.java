package com.aionemu.commons.scripting.url;

import com.aionemu.commons.scripting.ScriptClassLoader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 虚拟类URL连接处理器，用于处理编译后的脚本类的URL连接
 * Virtual class URL connection handler for handling URL connections of compiled script classes
 *
 * 继承自URLConnection，提供了对编译后脚本类字节码的访问功能
 * Extends URLConnection to provide access to bytecode of compiled script classes
 *
 * @author SunAion Team
 */
public class VirtualClassURLConnection extends URLConnection {
    /** 输入流，用于读取类字节码 / Input stream for reading class bytecode */
    private InputStream is;

    /**
     * 构造函数，使用URL和脚本类加载器初始化连接
     * Constructor to initialize connection with URL and script class loader
     *
     * @param url URL对象 / URL object
     * @param cl 脚本类加载器 / Script class loader
     */
    protected VirtualClassURLConnection(URL url, ScriptClassLoader cl) {
        super(url);
        this.is = new ByteArrayInputStream(cl.getByteCode(url.getHost()));
    }

    /**
     * 建立连接（此方法为空实现，因为连接在构造时已建立）
     * Establish connection (empty implementation as connection is established in constructor)
     *
     * @throws IOException 如果连接出错 / If connection fails
     */
    public void connect() throws IOException {
    }

    /**
     * 获取输入流以读取类字节码
     * Get input stream to read class bytecode
     *
     * @return 包含类字节码的输入流 / Input stream containing class bytecode
     * @throws IOException 如果获取输入流失败 / If getting input stream fails
     */
    public InputStream getInputStream() throws IOException {
        return this.is;
    }
}
