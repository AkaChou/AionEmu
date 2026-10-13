package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 经济类操作的事务边界契约测试。
 * 旧口径断言调用方手工持有 {@code Connection} 并逐字节匹配 JDBC 语句；事务归 Spring 后，契约变为
 * 「同一 {@code DatabaseAccess.transactionTemplate()} 边界内、由 Flex DAO 完成写入」。
 * Contract tests for the transaction boundary of economic operations. The legacy wording asserted a
 * caller-held {@code Connection} and byte-exact JDBC text; with Spring owning the transaction the
 * contract is "written by Flex DAOs inside one {@code DatabaseAccess.transactionTemplate()} boundary".
 */
class TransactionSafetyTest {

	@Test
	void brokerAndExchangeEconomicWritesShareOneSpringTransaction() throws IOException {
		String broker = source("gameserver/services/BrokerService.java");
		String exchange = source("gameserver/services/ExchangeService.java");

		assertTrue(broker.contains("DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {"));
		assertTrue(broker.contains("brokerDAO.storeInTransaction(brokerItem)"));
		assertTrue(broker.contains("inventoryDAO.storeInTransaction(inventorySave.items()"));
		assertTrue(exchange.contains("DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {"));
		assertTrue(exchange.contains("inventoryDAO.storeInTransaction(player1Items, player1Id, null, null)"));
		assertTrue(exchange.contains("inventoryDAO.storeInTransaction(player2Items, player2Id, null, null)"));
		assertTrue(exchange.contains("InventorySnapshot.capture(activePlayer)"));
		assertTrue(exchange.contains("activeInventory.restore(activePlayer)"));

		// 事务边界完全由 Spring 持有，调用方不再手工管理连接。
		// The transaction boundary belongs entirely to Spring; callers no longer manage connections by hand.
		assertFalse(broker.contains("setAutoCommit"));
		assertFalse(exchange.contains("setAutoCommit"));
		assertFalse(broker.contains("saveManager"));
		assertFalse(exchange.contains("saveManager"));
	}

	@Test
	void mailClaimsPersistTheRewardAndClearTheLetterTogether() throws IOException {
		String source = source("gameserver/services/mail/MailService.java");

		assertTrue(source.contains("DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {"));
		assertTrue(source.contains("inventoryDAO.storeInTransaction(items, player.getObjectId(), null, null)"));
		assertTrue(source.contains("mailDAO.storeLetterInTransaction(letter.getTimeStamp(), letter)"));
		assertTrue(source.contains("InventorySnapshot.capture(senderInventory)"));
		assertTrue(source.contains("inventorySnapshot.restore()"));
		// Spring 事务边界取代了调用方手工回滚。
		assertFalse(source.contains("con.rollback();"));
	}

	@Test
	void accountActivationAndLadderInitializationAreAtomic() throws IOException {
		String account = source("loginserver/persistence/dao/impl/AccountDataDaoImpl.java");
		String ladder = source("gameserver/persistence/mapper/LadderPlayerMapper.java");

		// 会员回退读的是同一行内的旧等级列，并由过期条件在语句内把关（单语句原子）。
		// The membership rollback reads the old tier from the same row, gated inside the statement by
		// the expiry condition (atomic in one statement).
		assertTrue(account.contains(".set(AccountDataPo::getMembership, oldMembership)"));
		assertTrue(account.contains(".and(AccountDataPo::getExpire).lt(QueryMethods.currentTimestamp())"));
		assertTrue(account.contains("po.setActivated((int) account.getActivated())"),
			"account activation must round-trip through the persistence entity");

		// 天梯累加依赖原子 upsert，保留注解 SQL（Flex 1.11.8 无 SQL 级 upsert）。
		// The ladder increment relies on an atomic upsert, so the annotated SQL stays
		// (Flex 1.11.8 has no SQL-level upsert).
		assertTrue(ladder.contains("ON DUPLICATE KEY UPDATE"));
		assertFalse(ladder.contains("checkExists("));
	}

	@Test
	void veteranRewardMailAndQueueRemovalShareTheMailTransaction() throws IOException {
		String service = source("gameserver/services/veteranreward/VeteranRewardsService.java");
		String dao = source("gameserver/persistence/dao/impl/VeteranRewardDaoImpl.java");
		String systemMail = source("gameserver/services/mail/SystemMailService.java");

		assertTrue(service.contains("veteranRewardsDAO.deleteInTransaction(rewardId)"));
		assertTrue(dao.contains("void deleteInTransaction(int rewardId)"));
		assertTrue(systemMail.contains("DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {"),
			"the injected TransactionAction must run inside the mail transaction boundary");
	}

	@Test
	void brokerUpdateRequiredStateIsActuallyRecorded() throws IOException {
		String source = source("gameserver/model/gameobjects/BrokerItem.java");
		assertTrue(source.contains("this.state = PersistentState.UPDATE_REQUIRED"));
	}

	private static String source(String relativePath) throws IOException {
		return Files.readString(Path.of("src/main/java/com/aionemu/" + relativePath));
	}

}
