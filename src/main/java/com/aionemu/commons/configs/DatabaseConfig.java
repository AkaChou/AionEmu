package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;

/**
 * 数据库连接与连接池配置
 * Database connection and connection-pool configuration
 */
public class DatabaseConfig {

    /**
     * JDBC 驱动类名
     * JDBC driver class name
     */
    @Property(key = "database.driver", defaultValue = "com.mysql.cj.jdbc.Driver")
    public static String DATABASE_DRIVER;

    /**
     * JDBC 连接 URL
     * JDBC connection URL
     */
    @Property(key = "database.url", defaultValue = "jdbc:mysql://localhost:3306/aion?useUnicode=true&characterEncoding=UTF-8&useSSL=false")
    public static String DATABASE_URL;

    /**
     * 数据库用户名
     * Database username
     */
    @Property(key = "database.user", defaultValue = "root")
    public static String DATABASE_USER;

    /**
     * 数据库密码
     * Database password
     */
    @Property(key = "database.password", defaultValue = "root")
    public static String DATABASE_PASSWORD;

    /**
     * 最大连接数
     * Maximum pool size
     */
    @Property(key = "database.maxconnections", defaultValue = "20")
    public static int DATABASE_MAXCONNECTIONS;

    /**
     * HikariCP 连接最大存活时间（毫秒）
     * HikariCP max lifetime in milliseconds
     */
    @Property(key = "database.hikari.maxLifetime", defaultValue = "1800000")
    public static long HIKARI_MAX_LIFETIME;

    /**
     * HikariCP 连接测试查询
     * HikariCP connection test query
     */
    @Property(key = "database.hikari.connectionTestQuery", defaultValue = "SELECT 1")
    public static String HIKARI_CONNECTION_TEST_QUERY;
}
