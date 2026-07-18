package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TransactionSafetyTest {

	@Test
	void questStoreOwnsTheOnlyCommitBoundary() throws IOException {
		String source = source("gameserver/dao/impl/PlayerQuestListDAO.java");
		assertEquals(1, occurrences(source, "con.commit();"));
		assertTrue(source.contains("con.rollback();"));
	}

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
		assertTrue(source.contains("inventoryDAO.storeInTransaction(con, items"));
		assertTrue(source.contains("mailDAO.storeLetterInTransaction(con, letter.getTimeStamp(), letter)"));
		assertTrue(source.contains("InventorySnapshot.capture(senderInventory)"));
		assertTrue(source.contains("inventorySnapshot.restore()"));
		assertTrue(source.contains("con.rollback();"));
	}

	@Test
	void accountActivationAndLadderInitializationAreAtomic() throws IOException {
		String account = source("loginserver/dao/impl/AccountDAO.java");
		String ladder = source("gameserver/dao/impl/LadderDAO.java");
		assertTrue(account.contains("activated = ?"));
		assertTrue(ladder.contains("ON DUPLICATE KEY UPDATE"));
		assertFalse(ladder.contains("checkExists("));
	}

	@Test
	void veteranRewardMailAndQueueRemovalShareTheMailTransaction() throws IOException {
		String service = source("gameserver/services/veteranreward/VeteranRewardsService.java");
		String dao = source("gameserver/dao/impl/VeteranRewardsDAO.java");
		assertTrue(service.contains("getDAO().deleteInTransaction(con, rewardId)"));
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

	private static int occurrences(String source, String needle) {
		return (source.length() - source.replace(needle, "").length()) / needle.length();
	}
}
