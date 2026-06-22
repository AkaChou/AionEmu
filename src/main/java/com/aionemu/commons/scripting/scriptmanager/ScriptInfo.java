package com.aionemu.commons.scripting.scriptmanager;

import java.io.File;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 脚本信息类，用于存储和管理脚本的配置信息
 * Script information class for storing and managing script configuration information
 *
 * 该类使用JAXB注解实现XML配置文件的映射，包含以下主要信息：
 * This class uses JAXB annotations to implement XML configuration file mapping, containing the following main information:
 * 1. 脚本根目录 / Script root directory
 * 2. 依赖库文件列表 / List of dependent library files
 * 3. 子脚本信息列表 / List of child script information
 * 4. 编译器类名 / Compiler class name
 *
 * @author SunAion Team
 */
@XmlRootElement(name = "scriptinfo")
@XmlAccessorType(XmlAccessType.NONE)
public class ScriptInfo {
    
    /**
     * 脚本根目录，必填属性
     * Script root directory, required attribute
     */
    @XmlAttribute(required = true)
    private File root;

    /**
     * 依赖库文件列表
     * List of dependent library files
     */
    @XmlElement(name = "library")
    private List<File> libraries;

    /**
     * 子脚本信息列表
     * List of child script information
     */
    @XmlElement(name = "scriptinfo")
    private List<ScriptInfo> scriptInfos;

    /**
     * 编译器类名
     * Compiler class name
     */
    @XmlElement(name = "compiler")
    private String compilerClass;

    /**
     * 构造函数，初始化默认编译器类
     * Constructor, initialize default compiler class
     */
    public ScriptInfo() {
        this.compilerClass = ScriptManager.DEFAULT_COMPILER_CLASS.getName();
    }

    /**
     * 获取脚本根目录
     * Get script root directory
     */
    public File getRoot() {
        return this.root;
    }

    /**
     * 设置脚本根目录
     * Set script root directory
     * @param root 根目录文件对象 / Root directory file object
     */
    public void setRoot(File root) {
        this.root = root;
    }

    /**
     * 获取依赖库文件列表
     * Get list of dependent library files
     */
    public List<File> getLibraries() {
        return this.libraries;
    }

    /**
     * 设置依赖库文件列表
     * Set list of dependent library files
     * @param libraries 库文件列表 / Library file list
     */
    public void setLibraries(List<File> libraries) {
        this.libraries = libraries;
    }

    /**
     * 获取子脚本信息列表
     * Get list of child script information
     */
    public List<ScriptInfo> getScriptInfos() {
        return this.scriptInfos;
    }

    /**
     * 设置子脚本信息列表
     * Set list of child script information
     * @param scriptInfos 子脚本信息列表 / Child script information list
     */
    public void setScriptInfos(List<ScriptInfo> scriptInfos) {
        this.scriptInfos = scriptInfos;
    }

    /**
     * 获取编译器类名
     * Get compiler class name
     */
    public String getCompilerClass() {
        return this.compilerClass;
    }

    /**
     * 设置编译器类名
     * Set compiler class name
     * @param compilerClass 编译器类名 / Compiler class name
     */
    public void setCompilerClass(String compilerClass) {
        this.compilerClass = compilerClass;
    }

    /**
     * 重写equals方法，通过比较根目录判断两个脚本信息是否相等
     * Override equals method to determine if two script information are equal by comparing root directories
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ScriptInfo that = (ScriptInfo)o;
            return this.root.equals(that.root);
        } else {
            return false;
        }
    }

    /**
     * 重写hashCode方法，使用根目录的哈希值
     * Override hashCode method using root directory's hash value
     */
    @Override
    public int hashCode() {
        return this.root.hashCode();
    }

    /**
     * 重写toString方法，返回脚本信息的字符串表示
     * Override toString method to return string representation of script information
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptInfo");
        sb.append("{root=").append(this.root);
        sb.append(", libraries=").append(this.libraries);
        sb.append(", compilerClass='").append(this.compilerClass).append('\'');
        sb.append(", scriptInfos=").append(this.scriptInfos);
        sb.append('}');
        return sb.toString();
    }
}
