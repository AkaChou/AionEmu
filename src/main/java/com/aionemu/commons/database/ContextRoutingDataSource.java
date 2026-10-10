package com.aionemu.commons.database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * 上下文感知的数据源路由器，根据当前服务上下文（Login / Game）动态路由到真实的连接池。
 * Context-aware routing DataSource that dynamically delegates to the active service context pool.
 */
public class ContextRoutingDataSource implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return DatabaseFactory.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DatabaseFactory.getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DatabaseFactory.getDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DatabaseFactory.getDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DatabaseFactory.getDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DatabaseFactory.getDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return DatabaseFactory.getDataSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return DatabaseFactory.getDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }
        return DatabaseFactory.getDataSource().isWrapperFor(iface);
    }
}
