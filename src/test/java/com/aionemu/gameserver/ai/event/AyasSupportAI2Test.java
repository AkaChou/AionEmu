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

	/**
	 * 应援按钮（HACTION_SETPRO1=10000）先于任务引擎处理，且收尾零发页、零关窗。
	 * 任务面（DD 接取面的 ≥1000 动作）一旦认领该动作，本 AI 的增益整链不触发
	 * （2026-10-06 实机 833671/833672）；关窗包则与参照 NPC 831031（Npc_SupportAI2，同样零发页）
	 * 不一致——实机同日复测：多发一条关窗包时客户端看不到 NPC 的施法动作，撤销后通过。
	 * The cheer button (10000) is handled before the quest engine and sends no dialog packet afterwards.
	 */
	@Test
	void cheerButtonOutranksTheQuestEngineAndSendsNoDialogPacket() throws IOException {
		String source = Files.readString(SOURCE);

		assertTrue(source.indexOf("dialogId == 10000") < source.indexOf("questEngine().onDialog"),
			"应援按钮必须先于任务引擎处理（任务面认领会劫走 buff）");
		assertFalse(source.contains("new SM_DIALOG_WINDOW(getObjectId(), 0)"),
			"应援增益后零发页、零关窗（对齐参照 NPC 831031 / Npc_SupportAI2，避免打断施法动作）");
	}
}
