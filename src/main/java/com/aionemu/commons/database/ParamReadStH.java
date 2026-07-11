package com.aionemu.commons.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 参数化查询接口
 * Interface for parameterized queries
 *
 * 该接口继承自 ReadStH 接口，用于支持带参数的数据库查询操作。
 * This interface extends ReadStH and is used to support database queries with parameters.
 *
 * 实现类需要实现 setParams 方法来设置 SQL 语句的参数值。
 * Implementing classes must implement the setParams method to set parameter values for SQL statements.
 */
public interface ParamReadStH extends ReadStH {
    /**
 * 设置 SQL 语句的参数
     * Set parameters for the SQL statement
     *
 * @param stmt 预处理 SQL 语句对象 / The PreparedStatement object
 * If a SQL error occurs while setting parameters。 / If a SQL error occurs while setting parameters.
     */
    void setParams(PreparedStatement stmt) throws SQLException;
}
