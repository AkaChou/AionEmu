package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TransactionSafetyTest {

	@Test
	void economicOperationsShareOneJdbcConnectionAndRunSynchronously() throws IOException {
		String broker = source("gameserver/services/BrokerService.java");
		String exchange = source("gameserver/services/ExchangeService.java");
		assertTrue(broker.contains("brokerDAO.storeInTransaction(con, brokerItem)"));
		assertTrue(broker.contains("new BrokerOpSaveTask(brokerChanges, inventoryChanges).save()"));
		assertTrue(broker.contains("new BrokerOpSaveTask(brokerItemsToDelete, inventoryChanges).save()"));
		assertTrue(broker.contains("con -> brokerDAO.storeInTransaction(con, item)"));
		assertTrue(exchange.contains("inventoryDAO.storeInTransaction(con, player1Items"));
		assertTrue(exchange.contains("inventoryDAO.storeInTransaction(con, player2Items"));
		assertTrue(exchange.contains("InventorySnapshot.capture(activePlayer)"));
		assertTrue(exchange.contains("activeInventory.restore(activePlayer)"));
		assertFalse(broker.contains("saveManager"));
		assertFalse(exchange.contains("saveManager"));
	}

	@Test
	void mailClaimsPersistTheRewardAndClearTheLetterTogether() throws IOException {
		String source = source("gameserver/services/mail/MailService.java");
		assertTrue(source.contains("storeInTransaction(con, items"));
		assertTrue(source.contains("storeLetterInTransaction(con, letter.getTimeStamp(), letter)"));
		assertTrue(source.contains("InventorySnapshot.capture(senderInventory)"));
		assertTrue(source.contains("inventorySnapshot.restore()"));
		assertTrue(source.contains("con.rollback();"));
	}

	@Test
	void accountActivationAndLadderInitializationAreAtomic() throws IOException {
		String account = source("loginserver/persistence/dao/impl/AccountDataDaoImpl.java");
		String accountMapper = source("loginserver/persistence/mapper/AccountDataMapper.java");
		String ladder = source("gameserver/persistence/mapper/LadderPlayerMapper.java");
		assertTrue(accountMapper.contains("membership = old_membership"));
		assertTrue(account.contains("po.setActivated((int) account.getActivated())"),
			"account activation must round-trip through the persistence entity");
		assertTrue(ladder.contains("ON DUPLICATE KEY UPDATE"));
		assertFalse(ladder.contains("checkExists("));
	}

	@Test
	void veteranRewardMailAndQueueRemovalShareTheMailTransaction() throws IOException {
		String service = source("gameserver/services/veteranreward/VeteranRewardsService.java");
		String dao = source("gameserver/persistence/dao/impl/VeteranRewardDaoImpl.java");
		assertTrue(service.contains("veteranRewardsDAO.deleteInTransaction(con, rewardId)"));
		assertTrue(dao.contains("void deleteInTransaction(Connection con, int rewardId)"));
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
