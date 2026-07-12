package com.aionemu.commons.database;


import com.aionemu.boot.i18n.I18n;
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

/**
 * 数据库工厂，按服务上下文管理 HikariCP 数据源
 * Database factory that manages HikariCP data sources per service context
 */
@Slf4j
public final class DatabaseFactory {

    private static final Map<String, DatabaseState> states = new ConcurrentHashMap<>();

    private DatabaseFactory() {}

    /**
     * 初始化当前服务上下文的数据库连接池
     * Initialize the database connection pool for the current service context
     */
    public static synchronized void init() {
        String context = ServiceContext.current();
        if (states.containsKey(context)) {
            return;
        }

        try {
            Class.forName(DatabaseConfig.DATABASE_DRIVER);
        } catch (Exception e) {
            log.error(I18n.get("log.a8dd0e7b9e78", e));
            throw new Error("DB Driver doesnt exist!");
        }

        DatabaseSchemaInitializer.initializeIfMissing();

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DatabaseConfig.DATABASE_DRIVER);
        config.setJdbcUrl(DatabaseConfig.DATABASE_URL);
        config.setUsername(DatabaseConfig.DATABASE_USER);
        config.setPassword(DatabaseConfig.DATABASE_PASSWORD);
        config.setMaximumPoolSize(DatabaseConfig.DATABASE_MAXCONNECTIONS);
		config.setConnectionTestQuery(DatabaseConfig.HIKARI_CONNECTION_TEST_QUERY);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
		config.setMaxLifetime(DatabaseConfig.HIKARI_MAX_LIFETIME);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            log.error(I18n.get("log.232091bdb3d8", e));
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
            log.error(I18n.get("log.5ea00f54f65b", DatabaseConfig.DATABASE_URL, e));
            throw new Error("DatabaseFactory not initialized!");
        }

        log.info(I18n.get("log.5c77a844d9d3", context));
    }

    /**
     * 从当前上下文连接池获取连接，并确保 autoCommit 为 true
     * Obtain a connection from the current context pool and ensure autoCommit is true
     *
     * JDBC connection
     *
     * @return
     * @throws SQLException 获取连接失败时 / When obtaining a connection fails
     */
    public static Connection getConnection() throws SQLException {
        Connection con = state().dataSource.getConnection();
        if (!con.getAutoCommit()) {
            log.error(I18n.get("log.94eef67a6a48"));
            con.setAutoCommit(true);
        }
        return con;
    }

    /**
     * 获取当前活跃连接数
     * Get the number of active connections
     *
     * @return 活跃连接数 / Active connection count
     */
    public int getActiveConnections() {
        return state().dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    /**
     * 获取当前空闲连接数
     * Get the number of idle connections
     *
     * @return 空闲连接数 / Idle connection count
     */
    public int getIdleConnections() {
        return state().dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    /**
     * 关闭并移除当前服务上下文的数据源
     * Close and remove the data source of the current service context
     */
    public static synchronized void shutdown() {
        String context = ServiceContext.current();
        DatabaseState state = states.remove(context);
        if (state != null && !state.dataSource.isClosed()) {
            try {
                state.dataSource.close();
            } catch (Exception e) {
                log.warn(I18n.get("log.79521306df38", e));
            }
        }
    }

    /**
     * 关闭 PreparedStatement 与 Connection
     * Close a PreparedStatement and a Connection
     *
     * @param st 预处理语句 / Prepared statement
     * @param con 数据库连接 / Database connection
     */
    public static void close(PreparedStatement st, Connection con) {
        close(st);
        close(con);
    }

    /**
     * 安全关闭 PreparedStatement
     * Safely close a PreparedStatement
     *
     * @param st 预处理语句 / Prepared statement
     */
    public static void close(PreparedStatement st) {
        if (st != null) {
            try {
                if (!st.isClosed()) {
                    st.close();
                }
            } catch (SQLException e) {
                log.error(I18n.get("log.a509eb4fdeff", e));
            }
        }
    }

    /**
     * 安全关闭 Connection，并恢复 autoCommit
     * Safely close a Connection and restore autoCommit
     *
     * @param con 数据库连接 / Database connection
     */
    public static void close(Connection con) {
        if (con != null) {
            try {
                if (!con.getAutoCommit()) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error(I18n.get("log.2bbe0501f216", e));
            }
            try {
                con.close();
            } catch (SQLException e) {
                log.error(I18n.get("log.082a33b1782b", e));
            }
        }
    }

    /**
     * 获取当前数据库产品名称
     * Get the current database product name
     *
     * @return 数据库名称 / Database name
     */
    public static String getDatabaseName() {
        return state().databaseName;
    }

    /**
     * 获取当前数据库主版本号
     * Get the current database major version
     *
     * Major version
     */
    public static int getDatabaseMajorVersion() {
        return state().databaseMajorVersion;
    }

    /**
     * 获取当前数据库次版本号
     * Get the current database minor version
     *
     * Minor version
     */
    public static int getDatabaseMinorVersion() {
        return state().databaseMinorVersion;
    }

    /**
     * 获取当前服务上下文对应的数据库状态
     * Get the database state for the current service context
     *
     * @return 数据库状态 / Database state
     */
    private static DatabaseState state() {
        DatabaseState state = states.get(ServiceContext.current());
        if (state == null) {
            throw new IllegalStateException("DatabaseFactory is not initialized for " + ServiceContext.current() + " service context");
        }
        return state;
    }

    /**
     * 单个服务上下文下的数据源与元数据状态
     * Data source and metadata state for a single service context
     */
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
