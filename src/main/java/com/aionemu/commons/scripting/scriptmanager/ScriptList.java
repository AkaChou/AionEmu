package com.aionemu.commons.scripting.scriptmanager;

import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 脚本列表类，用于管理多个脚本信息
 * Script list class for managing multiple script information
 *
 * 该类使用JAXB注解实现XML配置文件的映射，主要功能包括：
 * This class uses JAXB annotations to implement XML configuration file mapping, main features include:
 * 1. 存储多个脚本信息对象 / Store multiple script information objects
 * 2. 提供脚本信息的访问接口 / Provide access interface for script information
 * 3. 支持XML序列化和反序列化 / Support XML serialization and deserialization
 *
 * @author SunAion Team
 */
@XmlRootElement(name = "scriptlist")
@XmlAccessorType(XmlAccessType.NONE)
public class ScriptList {
    
    /**
     * 脚本信息集合
     * Collection of script information
     */
    @XmlElement(name = "scriptinfo", type = ScriptInfo.class)
    private Set<ScriptInfo> scriptInfos;

    /**
     * 获取脚本信息集合
     * Get collection of script information
     * @return 脚本信息集合 / Collection of script information
     */
    public Set<ScriptInfo> getScriptInfos() {
        return this.scriptInfos;
    }

    /**
     * 设置脚本信息集合
     * Set collection of script information
     * @param scriptInfos 脚本信息集合 / Collection of script information
     */
    public void setScriptInfos(Set<ScriptInfo> scriptInfos) {
        this.scriptInfos = scriptInfos;
    }

    /**
     * 重写toString方法，返回脚本列表的字符串表示
     * Override toString method to return string representation of script list
     * @return 脚本列表的字符串表示 / String representation of script list
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptList");
        sb.append("{scriptInfos=").append(this.scriptInfos);
        sb.append('}');
        return sb.toString();
    }
}
