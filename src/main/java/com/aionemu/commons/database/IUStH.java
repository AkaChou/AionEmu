package com.aionemu.commons.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 数据库插入和更新操作处理接口
 * Interface for handling database insert and update operations
 *
 * 该接口定义了处理数据库插入和更新操作的标准方法。
 * This interface defines the standard method for handling database insert and update operations.
 *
 * 实现类需要实现 handleInsertUpdate 方法来设置 SQL 语句的参数值并执行更新操作。
 * Implementing classes must implement the handleInsertUpdate method to set parameter values and execute the update operation.
 */
public interface IUStH {
    /**
     * 处理数据库的插入和更新操作
     * Handle database insert and update operations
     *
 * @param stmt 预处理 SQL 语句对象 / The PreparedStatement object
 * If a SQL error occurs while executing the update。
     */
    void handleInsertUpdate(PreparedStatement stmt) throws SQLException;
}
