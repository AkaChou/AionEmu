package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PlayerControllerEnterWorldOrderTest {

	@Test
	void stripsNpcBuffsBeforeApplyingInstanceEntryTransform() throws Exception {
		// 副本入场变身（污染暗道/秘密军工厂 21345..21348、克罗梅德试炼 19220/19270）的
		// dispel_category 都是 NPC_BUFF：剥离必须排在 InstanceService.onEnterInstance 之前，
		// 否则刚施加的变身会被同一方法末尾的清理解除，玩家进本不变身。
		// The instance entry transforms (Underpath/Factory 21345..21348, Kromedes Trial 19220/19270) all use
		// dispel_category=NPC_BUFF, so the strip must run before InstanceService.onEnterInstance; otherwise the
		// transform is undone by the cleanup at the end of the same method and the player enters untransformed.
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/controllers/PlayerController.java"));
		int start = source.indexOf("public void onEnterWorld()");
		String onEnterWorld = source.substring(start, source.indexOf("public void onLeaveWorld()", start));

		int strip = onEnterWorld.indexOf("DispelCategoryType.NPC_BUFF");
		int entry = onEnterWorld.indexOf("InstanceService.onEnterInstance(getOwner());");
		assertTrue(strip >= 0, "onEnterWorld 应包含 NPC_BUFF 剥离");
		assertTrue(entry >= 0, "onEnterWorld 应调用 InstanceService.onEnterInstance");
		assertTrue(strip < entry, "NPC_BUFF 剥离必须早于副本入场钩子");
	}
}
