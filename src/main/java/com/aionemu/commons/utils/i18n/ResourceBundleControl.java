package com.aionemu.commons.utils.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义 ResourceBundle 控制：按指定编码加载 .properties。
 * Custom ResourceBundle control that loads .properties with a chosen encoding.
 * <p>
 * 支持指定字符编码读取 .properties，避免默认 ISO-8859-1 限制。
 * Supports reading .properties with a specified encoding to overcome ISO-8859-1 limits.
 *
 * @author SoulKeeper
 */
public class ResourceBundleControl extends ResourceBundle.Control {

    /**
     * 资源文件编码，默认 UTF-8。
     * Resource file encoding, default UTF-8.
     */
    @Getter
    @Setter
    private String encoding = "UTF-8";

    /**
     * 默认构造（UTF-8）。
     * Default constructor (UTF-8).
     */
    public ResourceBundleControl() {}

    /**
     * 使用指定编码构造。
     * Construct with the given encoding.
     *
     * @param encoding 字符编码 / character encoding
     */
    public ResourceBundleControl(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 创建 ResourceBundle 实例（支持自定义编码的 properties）。
     * Create a ResourceBundle instance (properties with custom encoding).
     *
     * @param baseName 资源基名 / base name
     * @param locale 区域 / locale
     * @param format 格式（java.class 或 java.properties） / format (java.class or java.properties)
     * @param loader 类加载器 / class loader
     * @param reload 是否重新加载 / whether to reload
     * @return 资源包；无法加载时为 null / resource bundle, or null when not loadable
     * @throws IOException I/O 失败时 / on I/O failure
     * @throws IllegalAccessException 访问失败时 / on access failure
     * @throws InstantiationException 实例化失败时 / on instantiation failure
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

                if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                    bundle = newResourceBundle(bundleClass);
                } else {
                    throw new ClassCastException(bundleClass.getName() + " cannot be cast to ResourceBundle");
                }
            } catch (ClassNotFoundException ignored) {}
        } else if (format.equals("java.properties")) {
            String resourceName = toResourceName(bundleName, "properties");
            InputStreamReader isr = null;
            InputStream stream;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    } else {
                        stream = null;
                    }
                } else {
                    stream = null;
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }

            if (stream != null) {
                isr = new InputStreamReader(stream, encoding);
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
     * 通过无参构造实例化 ResourceBundle 子类。
     * Instantiate a ResourceBundle subclass via its default constructor.
     *
     * @param bundleClass 资源包类 / bundle class
     * @return 资源包实例 / bundle instance
     * @throws InstantiationException 实例化失败 / On instantiation failure
     */
    private ResourceBundle newResourceBundle(Class<? extends ResourceBundle> bundleClass)
            throws IllegalAccessException, InstantiationException {
        try {
            return bundleClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            InstantiationException ex = new InstantiationException("No default constructor for " + bundleClass.getName());
            ex.initCause(e);
            throw ex;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            InstantiationException ex = new InstantiationException("Failed to instantiate " + bundleClass.getName());
            ex.initCause(cause);
            throw ex;
        }
    }
}
