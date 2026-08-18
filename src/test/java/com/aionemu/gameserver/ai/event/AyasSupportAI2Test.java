package com.aionemu.gameserver.ai.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ayas 支援 NPC 对话增益的资源校验回归测试。
 * Regression coverage for resource validation in Ayas support NPC dialog buffs.
 */
class AyasSupportAI2Test {
	private static final Path SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/event/Ayas_SupportAI2.java");

	@Test
	void coversElyosAndAsmodianAyasNpcIds() throws IOException {
		String source = Files.readString(SOURCE);

		assertTrue(source.contains("case 833671:"));
		assertTrue(source.contains("case 833672:"));
		assertTrue(source.contains("case 833673:"));
		assertTrue(source.contains("case 833674:"));
	}

	@Test
	void bypassesUnmodeledNpcMpForDialogBuffs() throws IOException {
		String source = Files.readString(SOURCE);

		assertTrue(source.contains("getSkill(getOwner(), skillId, 1, player).useWithoutPropSkill()"));
		assertFalse(source.contains("getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill()"));
	}
}
