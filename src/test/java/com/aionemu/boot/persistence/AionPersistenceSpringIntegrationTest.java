package com.aionemu.boot.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.AionBootApplication;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.loginserver.dao.AccountDAO;

/**
 * 验证 Spring Boot 容器托管数据源、JdbcClient 与全库 DAO 的集成测试。
 * Integration tests verifying Spring Boot management of DataSources, JdbcClient, and all DAOs.
 */
class AionPersistenceSpringIntegrationTest {

	@Test
	void springContainerRegistersPersistenceInfrastructureAndDaos() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AionBootApplication.class)) {
			// 验证核心基础设施 Bean 已由 Spring 托管
			assertTrue(context.containsBean("dataSource"), "容器中应包含 dataSource Bean");
			assertTrue(context.containsBean("jdbcTemplate"), "容器中应包含 jdbcTemplate Bean");
			assertTrue(context.containsBean("jdbcClient"), "容器中应包含 jdbcClient Bean");
			assertTrue(context.containsBean("transactionManager"), "容器中应包含 transactionManager Bean");
			assertTrue(context.containsBean("transactionTemplate"), "容器中应包含 transactionTemplate Bean");

			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
			JdbcClient jdbcClient = context.getBean(JdbcClient.class);
			PlatformTransactionManager txManager = context.getBean(PlatformTransactionManager.class);
			TransactionTemplate txTemplate = context.getBean(TransactionTemplate.class);

			assertNotNull(dataSource);
			assertNotNull(jdbcTemplate);
			assertNotNull(jdbcClient);
			assertNotNull(txManager);
			assertNotNull(txTemplate);

			assertSame(DatabaseFactory.getJdbcTemplate(), jdbcTemplate);
			assertSame(DatabaseFactory.getJdbcClient(), jdbcClient);
			assertSame(DatabaseFactory.getTransactionManager(), txManager);
			assertSame(DatabaseFactory.getTransactionTemplate(), txTemplate);

			// 验证游戏服核心 DAO 由 Spring 容器管理
			PlayerDAO springPlayerDAO = context.getBean(PlayerDAO.class);
			assertNotNull(springPlayerDAO);
			assertInstanceOf(com.aionemu.gameserver.dao.impl.PlayerDAO.class, springPlayerDAO);

			// 验证登录服核心 DAO 由 Spring 容器管理
			AccountDAO springAccountDAO = context.getBean(AccountDAO.class);
			assertNotNull(springAccountDAO);
			assertInstanceOf(com.aionemu.loginserver.dao.impl.AccountDAO.class, springAccountDAO);

			// 验证同名跨服务 DAO (TaskFromDBDAO) 独立无冲突
			com.aionemu.gameserver.dao.TaskFromDBDAO gameTaskDAO = context.getBean(com.aionemu.gameserver.dao.TaskFromDBDAO.class);
			com.aionemu.loginserver.dao.TaskFromDBDAO loginTaskDAO = context.getBean(com.aionemu.loginserver.dao.TaskFromDBDAO.class);
			assertNotNull(gameTaskDAO);
			assertNotNull(loginTaskDAO);

			// 验证 DAOManager 已成功桥接 Spring 容器，getDAO 能够直接获取 Spring 托管的 Bean 单例
			PlayerDAO daoManagerPlayerDAO = DAOManager.getDAO(PlayerDAO.class);
			AccountDAO daoManagerAccountDAO = DAOManager.getDAO(AccountDAO.class);
			assertSame(springPlayerDAO, daoManagerPlayerDAO, "DAOManager 应直接返回 Spring 容器中的 PlayerDAO Bean");
			assertSame(springAccountDAO, daoManagerAccountDAO, "DAOManager 应直接返回 Spring 容器中的 AccountDAO Bean");
		}
	}
}
