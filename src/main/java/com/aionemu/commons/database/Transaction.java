package com.aionemu.commons.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库事务管理类
 * Database Transaction Management Class
 *
 * 该类提供了数据库事务管理功能，包括事务的提交、回滚和保存点操作。
 * This class provides database transaction management functionality, including transaction commit, rollback and savepoint operations.
 *
 * 使用该类可以确保多个数据库操作在一个事务中执行，保证数据的一致性。
 * Using this class ensures multiple database operations are executed within a single transaction, maintaining data consistency.
 */
public class Transaction {
    private static final Logger log = LoggerFactory.getLogger(Transaction.class);
    private Connection connection;

    /**
     * 构造函数，创建一个新的事务
     * Constructor, creates a new transaction
     *
     * @param con 数据库连接 / Database connection
     * @throws SQLException 如果设置自动提交模式失败 / If setting auto-commit mode fails
     */
    Transaction(Connection con) throws SQLException {
        this.connection = con;
        this.connection.setAutoCommit(false);
    }

    /**
     * 执行插入或更新操作
     * Execute an insert or update operation
     *
     * @param sql SQL语句 / SQL statement
     * @throws SQLException 如果执行SQL语句失败 / If executing SQL statement fails
     */
    public void insertUpdate(String sql) throws SQLException {
        this.insertUpdate(sql, null);
    }

    /**
     * 执行插入或更新操作，支持批处理
     * Execute an insert or update operation with batch support
     *
     * @param sql SQL语句 / SQL statement
     * @param iusth 批处理处理器 / Batch handler
     * @throws SQLException 如果执行SQL语句失败 / If executing SQL statement fails
     */
    public void insertUpdate(String sql, IUStH iusth) throws SQLException {
        PreparedStatement statement = this.connection.prepareStatement(sql);
        if (iusth != null) {
            iusth.handleInsertUpdate(statement);
        } else {
            statement.executeUpdate();
        }
    }

    /**
     * 设置保存点
     * Set a savepoint
     *
     * @param name 保存点名称 / Savepoint name
     * @return 保存点对象 / Savepoint object
     * @throws SQLException 如果设置保存点失败 / If setting savepoint fails
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        return this.connection.setSavepoint(name);
    }

    /**
     * 释放保存点
     * Release a savepoint
     *
     * @param savepoint 保存点对象 / Savepoint object
     * @throws SQLException 如果释放保存点失败 / If releasing savepoint fails
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.connection.releaseSavepoint(savepoint);
    }

    /**
     * 提交事务
     * Commit the transaction
     *
     * @throws SQLException 如果提交事务失败 / If committing transaction fails
     */
    public void commit() throws SQLException {
        this.commit(null);
    }

    /**
     * 提交事务，支持错误时回滚到指定保存点
     * Commit the transaction with rollback to specified savepoint on error
     *
     * @param rollBackToOnError 发生错误时回滚到的保存点 / Savepoint to rollback to on error
     * @throws SQLException 如果提交或回滚事务失败 / If committing or rolling back transaction fails
     */
    public void commit(Savepoint rollBackToOnError) throws SQLException {
        try {
            this.connection.commit();
        } catch (SQLException var5) {
            log.warn("Error while commiting transaction", var5);
            try {
                if (rollBackToOnError != null) {
                    this.connection.rollback(rollBackToOnError);
                } else {
                    this.connection.rollback();
                }
            } catch (SQLException var4) {
                log.error("Can't rollback transaction", var4);
            }
        }
        this.connection.setAutoCommit(true);
        this.connection.close();
    }
}
