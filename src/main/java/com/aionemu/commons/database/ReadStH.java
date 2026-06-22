package com.aionemu.commons.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库读取操作处理接口
 * Interface for handling database read operations
 *
 * 这是一个基础接口，定义了处理数据库查询结果集的标准方法。
 * This is a base interface that defines the standard method for handling database query result sets.
 *
 * 所有需要从数据库读取数据的操作都应该实现这个接口。
 * All operations that need to read data from the database should implement this interface.
 */
public interface ReadStH {
    /**
     * 处理数据库查询的结果集
     * Handle the result set from a database query
     *
     * @param resultSet 数据库查询返回的结果集 / The result set returned from the database query
     * @throws SQLException 如果处理结果集时发生SQL异常 / If a SQL error occurs while handling the result set
     */
    void handleRead(ResultSet resultSet) throws SQLException;
}
