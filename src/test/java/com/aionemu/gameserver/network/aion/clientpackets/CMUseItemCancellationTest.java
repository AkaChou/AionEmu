package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CMUseItemCancellationTest {

	@Test
	void typeZeroCancelsItemSkillAndReturnsBeforeStartingAnotherItem() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java"));
		int cancellationBranch = source.indexOf("if (type == 0)");
		int protectionBranch = source.indexOf("if (player.isProtectionActive())");

		assertTrue(cancellationBranch >= 0);
		assertTrue(source.indexOf("player.getController().cancelUseItem();", cancellationBranch) < protectionBranch);
		assertTrue(source.indexOf("SkillMethod.ITEM", cancellationBranch) < protectionBranch);
		assertTrue(source.indexOf("cancelCurrentSkill(castingSkill)", cancellationBranch) < protectionBranch);
		assertTrue(source.indexOf("return;", cancellationBranch) < protectionBranch);
	}
}
