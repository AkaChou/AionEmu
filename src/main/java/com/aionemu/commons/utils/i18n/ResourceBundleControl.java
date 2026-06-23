/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.utils.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This class allows us to read ResourceBundles with custom encodings, so we don't have write \\uxxxx symbols and use utilities like native2ascii to
 * convert files.
 * <p/>
 * <br>
 * Usage: For instance we want to load resource bundle "test" from current deirectory and use english locale. If locale not found, we will use default
 * file (and ignore default locale).
 * <p/>
 * < pre> URLClassLoader loader = new URLClassLoader(new URL[] { new File(&quot;.&quot;).toURI().toURL() });
 * <p/>
 * ResourceBundle rb = ResourceBundle.getBundle(&quot;test&quot;, Locale.ENGLISH, loader, new ResourceBundleControl(&quot;UTF-8&quot;));
 * <p/>
 * // English locale not found, use default if (!rb.getLocale().equals(Locale.ENGLISH)) { rb = ResourceBundle.getBundle(&quot;test&quot;, Locale.ROOT,
 * loader, new ResourceBundleControl(&quot;UTF-8&quot;)); }
 * <p/>
 * System.out.println(rb.getString(&quot;test&quot;));
 * </pre>
 *
 * @author SoulKeeper
 */
/**
 * 自定义资源包加载控制类 (Custom ResourceBundle Control)
 * 支持指定字符编码读取.properties文件，解决默认ISO-8859-1编码限制
 * Supports reading .properties files with specified encoding to overcome default ISO-8859-1 limitation
 * 
 * @author SoulKeeper
 */
public class ResourceBundleControl extends ResourceBundle.Control {
    
    /**
     * 资源文件编码格式 (Resource file encoding format)
     * 默认使用UTF-8编码 (Default to UTF-8 encoding)
     */
    private String encoding = "UTF-8";

    /**
     * 默认构造函数 (Default constructor)
     */
    public ResourceBundleControl() {}

    /**
     * 带编码参数的构造函数 (Constructor with encoding parameter)
     * @param encoding 字符编码格式，如UTF-8/GBK等 (Character encoding format, e.g. UTF-8/GBK)
     */
    public ResourceBundleControl(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 创建新的ResourceBundle实例 (Create new ResourceBundle instance)
     * 重写方法以支持自定义编码 (Override to support custom encoding)
     * 
     * @param baseName 资源文件基础名称 (Base name of the resource bundle)
     * @param locale 目标区域设置 (Target locale)
     * @param format 资源格式（只处理.properties类型）(Resource format, only handle .properties)
     * @param loader 类加载器 (Class loader)
     * @param reload 是否重新加载 (Whether to reload)
     * @return 加载后的ResourceBundle实例 (Loaded ResourceBundle instance)
     */
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, 
            ClassLoader loader, boolean reload) throws IOException, IllegalAccessException, 
            InstantiationException {
                
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        if (format.equals("java.class")) {
            try {
                @SuppressWarnings({"unchecked"})
                Class<? extends ResourceBundle> bundleClass = (Class<? extends ResourceBundle>) loader.loadClass(bundleName);
                
                // If the class isn't a ResourceBundle subclass, throw a
                // ClassCastException.
                if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                    bundle = bundleClass.newInstance();
                } else {
                    throw new ClassCastException(bundleClass.getName() + " cannot be cast to ResourceBundle");
                }
            } catch (ClassNotFoundException ignored) {}
        } else if (format.equals("java.properties")) {
            final String resourceName = toResourceName(bundleName, "properties");
            final ClassLoader classLoader = loader;
            final boolean reloadFlag = reload;
            InputStreamReader isr = null;
            InputStream stream;
            try {
                stream = AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                    
                    @Override
                    public InputStream run() throws IOException {
                        InputStream is = null;
                        if (reloadFlag) {
                            URL url = classLoader.getResource(resourceName);
                            if (url != null) {
                                URLConnection connection = url.openConnection();
                                if (connection != null) {
                                    // Disable caches to get fresh data for
                                    // reloading.
                                    connection.setUseCaches(false);
                                    is = connection.getInputStream();
                                }
                            }
                        } else {
                            is = classLoader.getResourceAsStream(resourceName);
                        }
                        return is;
                    }
                });
                
                /* 字符编码处理关键段 (Critical section for encoding handling) 
                 * 使用指定编码的InputStreamReader替代默认实现
                 * Using InputStreamReader with specified encoding instead of default
                 */
                if (stream != null) {
                    isr = new InputStreamReader(stream, encoding); // 应用自定义编码 Apply custom encoding
                }
            } catch (PrivilegedActionException e) {
                throw (IOException) e.getException();
            }
            if (isr != null) {
                try {
                    bundle = new PropertyResourceBundle(isr);
                } finally {
                    isr.close();
                }
            }
        } else {
            throw new IllegalArgumentException("unknown format: " + format);
        }
        return bundle;
    }

    /**
     * 获取当前编码格式 (Get current encoding format)
     * @return 当前使用的字符编码 (Currently used character encoding)
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * 设置新的编码格式 (Set new encoding format)
     * @param encoding 新的字符编码 (New character encoding)
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
