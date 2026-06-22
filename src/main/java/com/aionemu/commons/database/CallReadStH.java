package com.aionemu.commons.database;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * 存储过程参数设置接口
 * Interface for setting parameters in stored procedure calls
 *
 * 该接口继承自ReadStH接口，用于在调用存储过程时设置必要的参数。
 * This interface extends ReadStH and is used to set parameters when calling stored procedures.
 * 
 * 实现类需要实现setParams方法来设置存储过程的输入参数。
 * Implementing classes must implement the setParams method to set input parameters for the stored procedure.
 */
public interface CallReadStH extends ReadStH {
    /**
     * 设置存储过程的参数
     * Set parameters for the stored procedure
     *
     * @param stmt 可调用的SQL语句对象 / The CallableStatement object
     * @throws SQLException 如果设置参数时发生SQL异常 / If a SQL error occurs while setting parameters
     */
    void setParams(CallableStatement stmt) throws SQLException;
}
