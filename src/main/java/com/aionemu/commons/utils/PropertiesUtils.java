package com.aionemu.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

/**
 * Properties 文件加载与覆盖工具。
 * Properties file loading and override helpers.
 */
@UtilityClass
public class PropertiesUtils {

    /**
     * 从路径加载 Properties。
     * Load Properties from a file path.
     *
     * @param file 属性文件路径 / Properties file path
     * Loaded Properties
     * On read failure
     */
    public Properties load(String file) throws IOException {
        return load(new File(file));
    }

    /**
     * 从文件加载 Properties。
     * Load Properties from a File.
     *
     * @param file 属性文件 / Properties file
     * Loaded Properties
     * On read failure
     */
    public Properties load(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        Properties p = new Properties();
        p.load(fis);
        fis.close();
        return p;
    }

    /**
     * 从多个路径加载 Properties 数组。
     * Load a Properties array from multiple paths.
     *
     * @param files 路径数组 / File paths
     * Properties array
     *
     * @param files @throws IOException 任一文件读取失败 / On any read failure
     */
    public Properties[] load(String... files) throws IOException {
        Properties[] result = new Properties[files.length];

        for (int i = 0; i < result.length; ++i) {
            result[i] = load(files[i]);
        }

        return result;
    }

    /**
     * 从多个文件加载 Properties 数组。
     * Load a Properties array from multiple Files.
     *
     * @param files 文件数组 / File array
     * Properties array
     *
     * @param files @throws IOException 任一文件读取失败 / On any read failure
     */
    public Properties[] load(File... files) throws IOException {
        Properties[] result = new Properties[files.length];

        for (int i = 0; i < result.length; ++i) {
            result[i] = load(files[i]);
        }

        return result;
    }

    /**
     * 加载目录下全部 Properties（非递归）。
     * Load all Properties files under a directory (non-recursive).
     *
     * @param dir 目录路径 / Directory path
     * Properties array
     * On read failure
     */
    public Properties[] loadAllFromDirectory(String dir) throws IOException {
        return loadAllFromDirectory(new File(dir), false);
    }

    /**
     * 加载目录下全部 Properties（非递归）。
     * Load all Properties files under a directory (non-recursive).
     *
     * Directory
     * Properties array
     * On read failure
     */
    public Properties[] loadAllFromDirectory(File dir) throws IOException {
        return loadAllFromDirectory(dir, false);
    }

    /**
     * 加载目录下全部 Properties，可选递归。
     * Load all Properties files under a directory, optionally recursive.
     *
     * @param dir       目录路径 / Directory path
     * Whether recursive
     * Properties array
     * On read failure
     */
    public Properties[] loadAllFromDirectory(String dir, boolean recursive) throws IOException {
        return loadAllFromDirectory(new File(dir), recursive);
    }

    /**
     * 加载目录下全部 Properties，可选递归。
     * Load all Properties files under a directory, optionally recursive.
     *
     * Directory
     * Whether recursive
     * Properties array
     * On read failure
     */
    public Properties[] loadAllFromDirectory(File dir, boolean recursive) throws IOException {
        Collection<File> files = FileUtils.listFiles(dir, new String[]{"properties"}, recursive);
        return load((File[]) files.toArray(new File[files.size()]));
    }

    /**
     * 用一组 Properties 覆盖初始数组中的每一项。
     * Override each entry in the initial array with values from the override arrays.
     *
     * Initial array
     *
     * @param properties 覆盖源数组 / Override sources
     * @param properties @return 更新后的初始数组 / Updated initial array
     */
    public Properties[] overrideProperties(Properties[] initialProperties, Properties[] properties) {
        if (properties != null) {
            Properties[] arr$ = properties;
            int len$ = properties.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                Properties props = arr$[i$];
                overrideProperties(initialProperties, props);
            }
        }

        return initialProperties;
    }

    /**
     * 用单个 Properties 覆盖初始数组中的每一项。
     * Override each entry in the initial array with one Properties object.
     *
     * Initial array
     * Override source
     * @return 更新后的初始数组 / Updated initial array
     */
    public Properties[] overrideProperties(Properties[] initialProperties, Properties properties) {
        if (properties != null) {
            Properties[] arr$ = initialProperties;
            int len$ = initialProperties.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                Properties initialProps = arr$[i$];
                initialProps.putAll(properties);
            }
        }

        return initialProperties;
    }
}
