package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlayerServiceTitleRestoreTest {

	@Test
	void reappliesStoredBonusTitleOnPlayerLoad() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/player/PlayerService.java"));
		int bonusTitleGuard = source.indexOf("if (player.getCommonData().getBonusTitleId() > 0)");
		assertTrue(bonusTitleGuard >= 0, "Player loading must still restore stored bonus title stats.");

		int lifeStatsLoad = source.indexOf("DAOManager.getDAO(PlayerLifeStatsDAO.class).loadPlayerLifeStat(player);",
				bonusTitleGuard);
		assertTrue(lifeStatsLoad > bonusTitleGuard, "Bonus title restoration must run before life stats are loaded.");

		String restoreBlock = source.substring(bonusTitleGuard, lifeStatsLoad);
		assertTrue(restoreBlock.contains("player.getCommonData().getBonusTitleId(), true"),
				"Stored bonus title id must be reapplied; using display title id drops stat-only titles on relogin.");
	}
}
