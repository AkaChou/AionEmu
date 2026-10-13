package com.aionemu.gameserver.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 静态持久化资源定位器：把 Spring 容器管理的连接池与事务资源暴露给无法依赖注入的静态调用点。
 * Static persistence-resource locator exposing the Spring-managed DataSource and transaction resources
 * to static call sites that cannot use dependency injection.
 * <p>所有资源均来自 Boot 单一数据源：库参数只在
 * {@code aion/config/network/database.properties} 维护，由遗留属性后处理器在启动期翻译成
 * {@code spring.datasource.*}；事务内取连接会复用当前 Spring 事务的连接，从而让原生 JDBC 与 MyBatis-Flex
 * 共享同一事务。</p>
 * <p>Every resource is the Boot single DataSource: the DB settings live only in
 * {@code aion/config/network/database.properties} and are translated into {@code spring.datasource.*} at
 * startup by the legacy property environment post-processor; a connection obtained inside an active Spring
 * transaction reuses that transaction's connection, so raw JDBC and MyBatis-Flex share one transaction.</p>
 */
@Component
public final class DatabaseAccess {

	/** Boot 单数据源 / Boot single DataSource. */
	private static volatile DataSource dataSource;
	/** 事务感知的 JdbcClient / transaction-aware JdbcClient. */
	private static volatile JdbcClient jdbcClient;
	/** 事务感知的 JdbcTemplate / transaction-aware JdbcTemplate. */
	private static volatile JdbcTemplate jdbcTemplate;
	/** 事务管理器 / transaction manager. */
	private static volatile PlatformTransactionManager transactionManager;

	public DatabaseAccess(DataSource dataSource, JdbcClient jdbcClient, JdbcTemplate jdbcTemplate,
			PlatformTransactionManager transactionManager) {
		DatabaseAccess.dataSource = dataSource;
		DatabaseAccess.jdbcClient = jdbcClient;
		DatabaseAccess.jdbcTemplate = jdbcTemplate;
		DatabaseAccess.transactionManager = transactionManager;
	}

	/**
	 * 获取连接：事务上下文内复用当前事务连接，否则从池借出。
	 * Obtains a connection: the current transaction's connection when inside a transaction, otherwise pooled.
	 * @return JDBC 连接 / JDBC connection
	 * @throws SQLException 获取连接失败时 / when obtaining the connection fails
	 */
	public static Connection getConnection() throws SQLException {
		return DataSourceUtils.getConnection(require(dataSource, "DataSource"));
	}

	/**
	 * 归还未处于事务上下文的连接（事务连接的释放由事务管理器负责）。
	 * Releases a connection that is not part of a transaction (transaction connections are released by the manager).
	 * @param con JDBC 连接 / JDBC connection
	 */
	public static void release(Connection con) {
		DataSourceUtils.releaseConnection(con, dataSource);
	}

	/**
	 * 返回事务感知的 {@link JdbcClient}。
	 * Returns the transaction-aware {@link JdbcClient}.
	 * @return JdbcClient / JdbcClient
	 */
	public static JdbcClient jdbcClient() {
		return require(jdbcClient, "JdbcClient");
	}

	/**
	 * 返回事务感知的 {@link JdbcTemplate}。
	 * Returns the transaction-aware {@link JdbcTemplate}.
	 * @return JdbcTemplate / JdbcTemplate
	 */
	public static JdbcTemplate jdbcTemplate() {
		return require(jdbcTemplate, "JdbcTemplate");
	}

	/**
	 * 返回绑定事务管理器的新 {@link TransactionTemplate}。
	 * Returns a new {@link TransactionTemplate} bound to the transaction manager.
	 * @return 事务模版 / transaction template
	 */
	public static TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(require(transactionManager, "PlatformTransactionManager"));
	}

	/**
	 * 返回容器的事务管理器，供需要显式 begin/commit 边界的调用点使用。
	 * Returns the container's transaction manager for call sites that need an explicit begin/commit boundary.
	 * @return 事务管理器 / transaction manager
	 */
	public static PlatformTransactionManager transactionManager() {
		return require(transactionManager, "PlatformTransactionManager");
	}

	private static <T> T require(T value, String name) {
		if (value == null) {
			throw new IllegalStateException(name + " is not bound; Spring must initialize DatabaseAccess at startup");
		}
		return value;
	}
}
