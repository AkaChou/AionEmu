package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CMUseItemCancellationTest {

	@Test
	void typeZeroReturnsOnlyWhenItCancelsAnActiveItemUse() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java"));
		int cancellationBranch = source.indexOf("if (type == 0)");
		int protectionBranch = source.indexOf("if (player.isProtectionActive())");

		assertTrue(cancellationBranch >= 0);
		String cancellation = source.substring(cancellationBranch, protectionBranch);
		assertTrue(cancellation.contains("hasTask(TaskId.ITEM_USE)"));
		assertTrue(cancellation.contains("SkillMethod.ITEM"));
		assertTrue(cancellation.contains("if (hasScheduledItemUse)"));
		assertTrue(cancellation.contains("player.getController().cancelUseItem();"));
		assertTrue(cancellation.contains("if (hasItemSkillCast)"));
		assertTrue(cancellation.contains("cancelCurrentSkill(castingSkill)"));
		assertTrue(cancellation.contains("if (hasScheduledItemUse || hasItemSkillCast)"));
		assertTrue(cancellation.indexOf("return;") > cancellation.indexOf(
			"if (hasScheduledItemUse || hasItemSkillCast)"));
	}
}
