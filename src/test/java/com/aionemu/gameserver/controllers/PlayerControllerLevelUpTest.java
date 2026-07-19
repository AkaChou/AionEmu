package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlayerControllerLevelUpTest {

	@Test
	void learnsSkillsBeforeRefreshingPassivesAndGrantingLevelRewards() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/controllers/PlayerController.java"));
		String upgradePlayer = source.substring(source.indexOf("public void upgradePlayer()"),
				source.indexOf("public static final void reachedPlayerLvl", source.indexOf("public void upgradePlayer()")));

		int learnSkills = upgradePlayer.indexOf("SkillLearnService.addNewSkills(player);");
		assertTrue(learnSkills >= 0);
		assertTrue(learnSkills < upgradePlayer.indexOf("PacketSendUtility.broadcastPacket(player"));
		assertTrue(learnSkills < upgradePlayer.indexOf("player.getController().updatePassiveStats();"));
		assertTrue(learnSkills < upgradePlayer.indexOf("ItemService.addItem(player"));
	}
}
