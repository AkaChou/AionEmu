package com.aionemu.commons.database;

import com.aionemu.commons.configs.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseFactory.class);
    private static HikariDataSource dataSource;
    private static String databaseName;
    private static int databaseMajorVersion;
    private static int databaseMinorVersion;
    
    private DatabaseFactory() {}
    
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        
        try {
            Class.forName(DatabaseConfig.DATABASE_DRIVER);
        } catch (Exception e) {
            log.error("Error obtaining DB driver", e);
            throw new Error("DB Driver doesnt exist!");
        }
        
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DatabaseConfig.DATABASE_DRIVER);
        config.setJdbcUrl(DatabaseConfig.DATABASE_URL);
        config.setUsername(DatabaseConfig.DATABASE_USER);
        config.setPassword(DatabaseConfig.DATABASE_PASSWORD);
        config.setMaximumPoolSize(DatabaseConfig.DATABASE_MAXCONNECTIONS);
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Error while creating DB Connection pool", e);
            throw new Error("DatabaseFactory not initialized!", e);
        }
        
        try (Connection c = getConnection()) {
            DatabaseMetaData dmd = c.getMetaData();
            databaseName = dmd.getDatabaseProductName();
            databaseMajorVersion = dmd.getDatabaseMajorVersion();
            databaseMinorVersion = dmd.getDatabaseMinorVersion();
        } catch (Exception e) {
            log.error("Error with connection string: " + DatabaseConfig.DATABASE_URL, e);
            throw new Error("DatabaseFactory not initialized!");
        }
        
        log.info("Successfully connected to database with HikariCP");
    }
    
    public static Connection getConnection() throws SQLException {
        Connection con = dataSource.getConnection();
        if (!con.getAutoCommit()) {
            log.error("Connection Settings Error: Connection obtained from database factory should be in auto-commit mode. Forcing auto-commit to true.");
            con.setAutoCommit(true);
        }
        return con;
    }
    
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }
    
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
    
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                log.warn("Failed to shutdown DatabaseFactory", e);
            }
            dataSource = null;
        }
    }
    
    public static void close(PreparedStatement st, Connection con) {
        close(st);
        close(con);
    }
    
    public static void close(PreparedStatement st) {
        if (st != null) {
            try {
                if (!st.isClosed()) {
                    st.close();
                }
            } catch (SQLException e) {
                log.error("Can't close Prepared Statement", e);
            }
        }
    }
    
    public static void close(Connection con) {
        if (con != null) {
            try {
                if (!con.getAutoCommit()) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Failed to set autocommit to true while closing connection: ", e);
            }
            try {
                con.close();
            } catch (SQLException e) {
                log.error("DatabaseFactory: Failed to close database connection!", e);
            }
        }
    }
    
    public static String getDatabaseName() {
        return databaseName;
    }
    
    public static int getDatabaseMajorVersion() {
        return databaseMajorVersion;
    }
    
    public static int getDatabaseMinorVersion() {
        return databaseMinorVersion;
    }
}