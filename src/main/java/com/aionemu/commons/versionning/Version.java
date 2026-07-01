package com.aionemu.commons.versionning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import lombok.extern.slf4j.Slf4j;

/**
 * 版本信息管理类，用于获取和管理JAR包的版本相关信息
 * Version information management class for retrieving and managing JAR package version information
 */
@Slf4j
public class Version {
    
    // 版本控制相关属性 Version control related properties
    private String revision;     // 修订版本号 Revision number
    private String date;         // 日期 Date
    private String branch;       // 分支 Branch
    private String commitTime;   // 提交时间 Commit time

    /**
     * 默认构造函数
     * Default constructor
     */
    public Version() {
    }

    /**
     * 使用指定类初始化版本信息的构造函数
     * Constructor that initializes version information using specified class
     * 
     * @param c 用于加载版本信息的类 Class for loading version information
     */
    public Version(Class<?> c) {
        this.loadInformation(c);
    }

    /**
     * 从指定类所在的JAR文件中加载版本信息
     * Load version information from the JAR file containing the specified class
     * 
     * @param c 用于定位JAR文件的类 Class used to locate the JAR file
     */
    public void loadInformation(Class<?> c) {
        File jarName = null;

        try {
            jarName = Locator.getClassSource(c);
            if (jarName == null) {
                useUnknownInformation();
                log.warn("Unable to get Soft information for {} from class source '{}'", c.getName(), jarName);
                return;
            }
            if (!jarName.isFile()) {
                useUnknownInformation();
                return;
            }
            try (JarFile jarFile = new JarFile(jarName)) {
                Attributes attrs = jarFile.getManifest().getMainAttributes();
                this.revision = this.getAttribute("Revision", attrs);
                this.date = this.getAttribute("Date", attrs);
                this.branch = this.getAttribute("Branch", attrs);
                this.commitTime = this.getAttribute("CommitTime", attrs);
            }
        } catch (IOException | RuntimeException var5) {
            useUnknownInformation();
            log.error("Unable to get Soft information\nFile name '" + (jarName == null ? "null" : jarName.getAbsolutePath()) + "' isn't a valid jar", var5);
        }
    }

    private void useUnknownInformation() {
        this.revision = "Unknown Revision";
        this.date = "Unknown Date";
        this.branch = "Unknown Branch";
        this.commitTime = "Unknown CommitTime";
    }

    /**
     * 将JAR文件的版本信息传输到指定文件
     * Transfer version information from JAR file to specified file
     * 
     * @param jarName JAR文件名 JAR filename
     * @param type 类型信息 Type information
     * @param fileToWrite 要写入的目标文件 Target file to write
     */
    public void transferInfo(String jarName, String type, File fileToWrite) {
        try {
            if (!fileToWrite.exists()) {
                log.error("Unable to Find File :" + fileToWrite.getName() + " Please Update your " + type);
                return;
            }

            JarFile jarFile = new JarFile("./" + jarName);
            Manifest manifest = jarFile.getManifest();
            OutputStream fos = new FileOutputStream(fileToWrite);
            manifest.write(fos);
            fos.close();
        } catch (IOException var7) {
            log.error("Error, " + var7);
        }
    }

    /**
     * 获取修订版本号
     * Get revision number
     * 
     * @return 修订版本号 Revision number
     */
    public final String getRevision() {
        return this.revision;
    }

    /**
     * 获取日期
     * Get date
     * 
     * @return 日期 Date
     */
    public final String getDate() {
        return this.date;
    }

    /**
     * 获取分支名称
     * Get branch name
     * 
     * @return 分支名称 Branch name
     */
    public final String getBranch() {
        return this.branch;
    }

    /**
     * 获取提交时间
     * Get commit time
     * 
     * @return 提交时间 Commit time
     */
    public final String getCommitTime() {
        return this.commitTime;
    }

    /**
     * 从属性集合中获取指定属性值
     * Get specified attribute value from attributes collection
     * 
     * @param attribute 属性名称 Attribute name
     * @param attrs 属性集合 Attributes collection
     * @return 属性值，如果不存在则返回"Unknown "加属性名
     *         Attribute value, returns "Unknown " + attribute name if not exists
     */
    private final String getAttribute(String attribute, Attributes attrs) {
        String date = attrs.getValue(attribute);
        return date != null ? date : "Unknown " + attribute;
    }
}
