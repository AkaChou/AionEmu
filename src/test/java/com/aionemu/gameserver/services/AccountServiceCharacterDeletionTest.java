package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import org.junit.jupiter.api.Test;

class AccountServiceCharacterDeletionTest {

	@Test
	void keepsCharacterUntilDeletionTimestampIsReached() {
		long currentTimeMillis = 1_800_000_000_000L;

		assertFalse(AccountService.isDeletionDue(null, currentTimeMillis));
		assertFalse(AccountService.isDeletionDue(new Timestamp(currentTimeMillis + 600_000), currentTimeMillis));
		assertTrue(AccountService.isDeletionDue(new Timestamp(currentTimeMillis), currentTimeMillis));
		assertTrue(AccountService.isDeletionDue(new Timestamp(currentTimeMillis - 1), currentTimeMillis));
	}

	@Test
	void relogKeepsCharacterWhoseDeletionIsStillPending() {
		PlayerCommonData commonData = new PlayerCommonData(1001);
		commonData.setRace(Race.ELYOS);
		PlayerAccountData playerAccountData = new PlayerAccountData(commonData, null, null, List.of(), null);
		playerAccountData.setDeletionDate(new Timestamp(
				System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
		Account account = new Account(1);
		account.addPlayerAccountData(playerAccountData);

		AccountService.removeDeletedCharacters(account);

		assertSame(playerAccountData, account.getPlayerAccountData(commonData.getPlayerObjId()));
	}
}
