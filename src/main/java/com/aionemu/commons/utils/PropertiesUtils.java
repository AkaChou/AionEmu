package com.aionemu.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

/**
 * 属性文件工具类，提供加载和处理Properties文件的功能
 * Properties file utility class providing functions for loading and processing Properties files
 */
public class PropertiesUtils {
   /**
    * 从指定文件路径加载Properties
    * Load Properties from specified file path
    *
    * @param file 属性文件路径
    *             Properties file path
    * @return 加载的Properties对象
    *         Loaded Properties object
    * @throws IOException 如果文件读取失败
    *                     If file reading fails
    */
   public static Properties load(String file) throws IOException {
      return load(new File(file));
   }

   /**
    * 从File对象加载Properties
    * Load Properties from File object
    *
    * @param file 属性文件对象
    *             Properties File object
    * @return 加载的Properties对象
    *         Loaded Properties object
    * @throws IOException 如果文件读取失败
    *                     If file reading fails
    */
   public static Properties load(File file) throws IOException {
      FileInputStream fis = new FileInputStream(file);
      Properties p = new Properties();
      p.load(fis);
      fis.close();
      return p;
   }

   /**
    * 从多个文件路径加载Properties数组
    * Load Properties array from multiple file paths
    *
    * @param files 属性文件路径数组
    *              Array of Properties file paths
    * @return 加载的Properties对象数组
    *         Array of loaded Properties objects
    * @throws IOException 如果任何文件读取失败
    *                     If any file reading fails
    */
   public static Properties[] load(String... files) throws IOException {
      Properties[] result = new Properties[files.length];

      for(int i = 0; i < result.length; ++i) {
         result[i] = load(files[i]);
      }

      return result;
   }

   /**
    * 从多个File对象加载Properties数组
    * Load Properties array from multiple File objects
    *
    * @param files 属性文件对象数组
    *              Array of Properties File objects
    * @return 加载的Properties对象数组
    *         Array of loaded Properties objects
    * @throws IOException 如果任何文件读取失败
    *                     If any file reading fails
    */
   public static Properties[] load(File... files) throws IOException {
      Properties[] result = new Properties[files.length];

      for(int i = 0; i < result.length; ++i) {
         result[i] = load(files[i]);
      }

      return result;
   }

   /**
    * 从指定目录加载所有Properties文件
    * Load all Properties files from specified directory
    *
    * @param dir 目录路径
    *            Directory path
    * @return 加载的Properties对象数组
    *         Array of loaded Properties objects
    * @throws IOException 如果目录读取失败
    *                     If directory reading fails
    */
   public static Properties[] loadAllFromDirectory(String dir) throws IOException {
      return loadAllFromDirectory(new File(dir), false);
   }

   public static Properties[] loadAllFromDirectory(File dir) throws IOException {
      return loadAllFromDirectory(dir, false);
   }

   public static Properties[] loadAllFromDirectory(String dir, boolean recursive) throws IOException {
      return loadAllFromDirectory(new File(dir), recursive);
   }

   /**
    * 从指定目录加载所有Properties文件，可选是否递归处理子目录
    * Load all Properties files from specified directory with optional recursive processing
    *
    * @param dir 目录对象
    *            Directory object
    * @param recursive 是否递归处理子目录
    *                  Whether to process subdirectories recursively
    * @return 加载的Properties对象数组
    *         Array of loaded Properties objects
    * @throws IOException 如果目录读取失败
    *                     If directory reading fails
    */
   public static Properties[] loadAllFromDirectory(File dir, boolean recursive) throws IOException {
      Collection<File> files = FileUtils.listFiles(dir, new String[]{"properties"}, recursive);
      return load((File[])files.toArray(new File[files.size()]));
   }

   /**
    * 使用新的Properties数组覆盖初始Properties数组的值
    * Override values in initial Properties array with new Properties array
    *
    * @param initialProperties 初始Properties数组
    *                         Initial Properties array
    * @param properties 用于覆盖的Properties数组
    *                   Properties array for overriding
    * @return 更新后的Properties数组
    *         Updated Properties array
    */
   public static Properties[] overrideProperties(Properties[] initialProperties, Properties[] properties) {
      if (properties != null) {
         Properties[] arr$ = properties;
         int len$ = properties.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Properties props = arr$[i$];
            overrideProperties(initialProperties, props);
         }
      }

      return initialProperties;
   }

   /**
    * 使用单个Properties对象覆盖Properties数组的值
    * Override values in Properties array with single Properties object
    *
    * @param initialProperties 初始Properties数组
    *                         Initial Properties array
    * @param properties 用于覆盖的Properties对象
    *                   Properties object for overriding
    * @return 更新后的Properties数组
    *         Updated Properties array
    */
   public static Properties[] overrideProperties(Properties[] initialProperties, Properties properties) {
      if (properties != null) {
         Properties[] arr$ = initialProperties;
         int len$ = initialProperties.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Properties initialProps = arr$[i$];
            initialProps.putAll(properties);
         }
      }

      return initialProperties;
   }
}
