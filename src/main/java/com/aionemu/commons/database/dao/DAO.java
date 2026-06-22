package com.aionemu.commons.database.dao;

/**
 * 数据访问对象(DAO)接口
 * Data Access Object (DAO) Interface
 * 
 * 这个接口定义了所有DAO实现类必须遵循的基本契约。它提供了获取类名和数据库兼容性检查的方法。
 * This interface defines the basic contract that all DAO implementations must follow.
 * It provides methods for getting class name and checking database compatibility.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public interface DAO {

    /**
     * 获取当前DAO实现类的类名
     * Gets the class name of current DAO implementation
     *
     * @return 当前DAO实现类的完全限定名 / Fully qualified name of current DAO implementation
     */
    String getClassName();

    /**
     * 检查当前DAO实现是否支持指定的数据库版本
     * Checks if current DAO implementation supports specified database version
     *
     * @param database 数据库名称 / Database name
     * @param majorVersion 数据库主版本号 / Database major version
     * @param minorVersion 数据库次版本号 / Database minor version
     * @return 如果支持返回true，否则返回false / Returns true if supported, false otherwise
     */
    boolean supports(String database, int majorVersion, int minorVersion);
}
