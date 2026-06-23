package com.aionemu.commons.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库操作工具类
 * Database Operation Utility Class
 *
 * 该类提供了一系列静态方法用于执行数据库操作，包括查询、存储过程调用、插入和更新等。
 * This class provides a series of static methods for executing database operations,
 * including queries, stored procedure calls, inserts and updates.
 *
 * 所有数据库操作都通过DatabaseFactory获取连接，并确保正确关闭资源。
 * All database operations obtain connections through DatabaseFactory and ensure proper resource cleanup.
 */
public final class DB {
    protected static final Logger log = LoggerFactory.getLogger(DB.class);

    private DB() {
    }

    /**
     * 执行查询操作
     * Execute a select query
     *
     * @param query 查询SQL语句 / The SQL query statement
     * @param reader 结果集处理器 / The result set handler
     * @return 查询是否成功 / Whether the query was successful
     */
    public static boolean select(String query, ReadStH reader) {
        return select(query, reader, null);
    }

    /**
     * 执行查询操作，支持错误消息
     * Execute a select query with error message support
     *
     * @param query 查询SQL语句 / The SQL query statement
     * @param reader 结果集处理器 / The result set handler
     * @param errMsg 错误消息 / Error message
     * @return 查询是否成功 / Whether the query was successful
     */
    public static boolean select(String query, ReadStH reader, String errMsg) {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareStatement(query);
            if (reader instanceof ParamReadStH) {
                ((ParamReadStH)reader).setParams(stmt);
            }

            ResultSet rset = stmt.executeQuery();
            reader.handleRead(rset);
            return true;
        } catch (Exception var17) {
            if (errMsg == null) {
                log.warn("Error executing select query " + var17, var17);
            } else {
                log.warn(errMsg + " " + var17, var17);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception var16) {
                log.warn("Failed to close DB connection " + var16, var16);
            }
        }
    }

    /**
     * 调用存储过程
     * Call a stored procedure
     *
     * @param query 存储过程调用语句 / The stored procedure call statement
     * @param reader 结果集处理器 / The result set handler
     * @return 调用是否成功 / Whether the call was successful
     */
    public static boolean call(String query, ReadStH reader) {
        return call(query, reader, null);
    }

    /**
     * 调用存储过程，支持错误消息
     * Call a stored procedure with error message support
     *
     * @param query 存储过程调用语句 / The stored procedure call statement
     * @param reader 结果集处理器 / The result set handler
     * @param errMsg 错误消息 / Error message
     * @return 调用是否成功 / Whether the call was successful
     */
    public static boolean call(String query, ReadStH reader, String errMsg) {
        Connection con = null;
        CallableStatement stmt = null;

        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareCall(query);
            if (reader instanceof CallReadStH) {
                ((CallReadStH)reader).setParams(stmt);
            }

            ResultSet rset = stmt.executeQuery();
            reader.handleRead(rset);
            return true;
        } catch (Exception var17) {
            if (errMsg == null) {
                log.warn("Error calling stored procedure " + var17, var17);
            } else {
                log.warn(errMsg + " " + var17, var17);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception var16) {
                log.warn("Failed to close DB connection " + var16, var16);
            }
        }
    }

    /**
     * 执行插入或更新操作
     * Execute an insert or update operation
     *
     * @param query SQL语句 / The SQL statement
     * @return 操作是否成功 / Whether the operation was successful
     */
    public static boolean insertUpdate(String query) {
        return insertUpdate(query, null, null);
    }

    /**
     * 执行插入或更新操作，支持错误消息
     * Execute an insert or update operation with error message support
     *
     * @param query SQL语句 / The SQL statement
     * @param errMsg 错误消息 / Error message
     * @return 操作是否成功 / Whether the operation was successful
     */
    public static boolean insertUpdate(String query, String errMsg) {
        return insertUpdate(query, null, errMsg);
    }

    /**
     * 执行插入或更新操作，支持批处理
     * Execute an insert or update operation with batch support
     *
     * @param query SQL语句 / The SQL statement
     * @param batch 批处理处理器 / The batch handler
     * @return 操作是否成功 / Whether the operation was successful
     */
    public static boolean insertUpdate(String query, IUStH batch) {
        return insertUpdate(query, batch, null);
    }

    /**
     * 执行插入或更新操作，支持批处理和错误消息
     * Execute an insert or update operation with batch and error message support
     *
     * @param query SQL语句 / The SQL statement
     * @param batch 批处理处理器 / The batch handler
     * @param errMsg 错误消息 / Error message
     * @return 操作是否成功 / Whether the operation was successful
     */
    public static boolean insertUpdate(String query, IUStH batch, String errMsg) {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareStatement(query);
            if (batch != null) {
                batch.handleInsertUpdate(stmt);
            } else {
                stmt.executeUpdate();
            }
            return true;
        } catch (Exception var16) {
            if (errMsg == null) {
                log.warn("Failed to execute IU query " + var16, var16);
            } else {
                log.warn(errMsg + " " + var16, var16);
            }
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception var15) {
                log.warn("Failed to close DB connection " + var15, var15);
            }
        }
    }

    /**
     * 开始一个新的事务
     * Begin a new transaction
     *
     * @return 事务对象 / Transaction object
     * @throws SQLException 如果创建事务失败 / If creating transaction fails
     */
    public static Transaction beginTransaction() throws SQLException {
        Connection con = DatabaseFactory.getConnection();
        return new Transaction(con);
    }

    /**
     * 创建预处理语句
     * Create a prepared statement
     *
     * @param sql SQL语句 / The SQL statement
     * @return 预处理语句对象 / PreparedStatement object
     */
    public static PreparedStatement prepareStatement(String sql) {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * 创建预处理语句，支持结果集类型和并发性设置
     * Create a prepared statement with result set type and concurrency settings
     *
     * @param sql SQL语句 / The SQL statement
     * @param resultSetType 结果集类型 / Result set type
     * @param resultSetConcurrency 结果集并发性 / Result set concurrency
     * @return 预处理语句对象 / PreparedStatement object
     */
    public static PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
        Connection c = null;
        PreparedStatement ps = null;

        try {
            c = DatabaseFactory.getConnection();
            ps = c.prepareStatement(sql, resultSetType, resultSetConcurrency);
        } catch (Exception var8) {
            log.error("Can't create PreparedStatement for query: " + sql, var8);
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException var7) {
                    log.error("Can't close connection after exception", var7);
                }
            }
        }

        return ps;
    }

    /**
     * 执行更新操作
     * Execute an update operation
     *
     * @param statement 预处理语句对象 / PreparedStatement object
     * @return 更新的记录数，失败返回-1 / Number of records updated, -1 if failed
     */
    public static int executeUpdate(PreparedStatement statement) {
        try {
            return statement.executeUpdate();
        } catch (Exception var2) {
            log.error("Can't execute update for PreparedStatement", var2);
            return -1;
        }
    }

    /**
     * 执行更新操作并关闭语句
     * Execute an update operation and close the statement
     *
     * @param statement 预处理语句对象 / PreparedStatement object
     */
    public static void executeUpdateAndClose(PreparedStatement statement) {
        executeUpdate(statement);
        close(statement);
    }

    /**
     * 执行查询操作
     * Execute a query operation
     *
     * @param statement 预处理语句对象 / PreparedStatement object
     * @return 结果集对象 / ResultSet object
     */
    public static ResultSet executeQuerry(PreparedStatement statement) {
        ResultSet rs = null;
        try {
            rs = statement.executeQuery();
        } catch (Exception var3) {
            log.error("Error while executing query", var3);
        }
        return rs;
    }

    /**
     * 关闭预处理语句
     * Close the prepared statement
     *
     * @param statement 预处理语句对象 / PreparedStatement object
     */
    public static void close(PreparedStatement statement) {
        try {
            if (statement.isClosed()) {
                log.warn("Attempt to close PreparedStatement that is closed already", new Exception());
                return;
            }
            Connection c = statement.getConnection();
            statement.close();
            c.close();
        } catch (Exception var2) {
            log.error("Error while closing PreparedStatement", var2);
        }
    }
}
