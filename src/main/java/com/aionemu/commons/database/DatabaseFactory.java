package com.aionemu.commons.database;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.services.ServiceContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatabaseFactory {
    
    private static final Map<String, DatabaseState> states = new ConcurrentHashMap<>();
    
    private DatabaseFactory() {}
    
    public static synchronized void init() {
        String context = ServiceContext.current();
        if (states.containsKey(context)) {
            return;
        }
        
        try {
            Class.forName(DatabaseConfig.DATABASE_DRIVER);
        } catch (Exception e) {
            log.error("Error obtaining DB driver", e);
            throw new Error("DB Driver doesnt exist!");
        }

        DatabaseSchemaInitializer.initializeIfMissing();
        
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
        
        HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Error while creating DB Connection pool", e);
            throw new Error("DatabaseFactory not initialized!", e);
        }
        
        DatabaseState state = new DatabaseState(dataSource);
        states.put(context, state);
        try (Connection c = getConnection()) {
            DatabaseMetaData dmd = c.getMetaData();
            state.databaseName = dmd.getDatabaseProductName();
            state.databaseMajorVersion = dmd.getDatabaseMajorVersion();
            state.databaseMinorVersion = dmd.getDatabaseMinorVersion();
        } catch (Exception e) {
            states.remove(context);
            dataSource.close();
            log.error("Error with connection string: " + DatabaseConfig.DATABASE_URL, e);
            throw new Error("DatabaseFactory not initialized!");
        }
        
        log.info("Successfully connected to database with HikariCP for {} service context", context);
    }
    
    public static Connection getConnection() throws SQLException {
        Connection con = state().dataSource.getConnection();
        if (!con.getAutoCommit()) {
            log.error("Connection Settings Error: Connection obtained from database factory should be in auto-commit mode. Forcing auto-commit to true.");
            con.setAutoCommit(true);
        }
        return con;
    }
    
    public int getActiveConnections() {
        return state().dataSource.getHikariPoolMXBean().getActiveConnections();
    }
    
    public int getIdleConnections() {
        return state().dataSource.getHikariPoolMXBean().getIdleConnections();
    }
    
    public static synchronized void shutdown() {
        String context = ServiceContext.current();
        DatabaseState state = states.remove(context);
        if (state != null && !state.dataSource.isClosed()) {
            try {
                state.dataSource.close();
            } catch (Exception e) {
                log.warn("Failed to shutdown DatabaseFactory", e);
            }
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
        return state().databaseName;
    }
    
    public static int getDatabaseMajorVersion() {
        return state().databaseMajorVersion;
    }
    
    public static int getDatabaseMinorVersion() {
        return state().databaseMinorVersion;
    }

    private static DatabaseState state() {
        DatabaseState state = states.get(ServiceContext.current());
        if (state == null) {
            throw new IllegalStateException("DatabaseFactory is not initialized for " + ServiceContext.current() + " service context");
        }
        return state;
    }

    private static final class DatabaseState {
        private final HikariDataSource dataSource;
        private String databaseName;
        private int databaseMajorVersion;
        private int databaseMinorVersion;

        private DatabaseState(HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }
    }
}
