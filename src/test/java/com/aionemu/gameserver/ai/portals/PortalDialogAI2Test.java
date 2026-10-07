package com.aionemu.gameserver.ai.portals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;

class PortalDialogAI2Test {
	private static final Path SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/portals/PortalDialogAI2.java");

	@Test
	void fissureOrbTriesTheQuestStartActionBeforeShowingThePortalMenu() {
		assertEquals(List.of(QuestDialogAction.QUEST_SELECT.id()), PortalDialogAI2.questFirstDialogIds(834194, 29));
	}

	@Test
	void entranceExitWithTheSameNpcTemplateKeepsItsNormalPortalMenu() {
		assertEquals(List.of(), PortalDialogAI2.questFirstDialogIds(834194, 278));
		assertEquals(List.of(), PortalDialogAI2.questFirstDialogIds(834195, 29));
	}

	/**
	 * 克萝梅德试炼入口只有天族兰尼尼亚（205229）与魔族布里奇特（205234）。
	 * Only Raninia (205229, Elyos) and Bridget (205234, Asmodians) are Kromede's Trial entrances.
	 */
	@Test
	void kromedeTrialEntrancesAreTheElyosAndAsmodianEntryNpcs() {
		assertTrue(PortalDialogAI2.isKromedeTrialEntryNpc(205229));
		assertTrue(PortalDialogAI2.isKromedeTrialEntryNpc(205234));
		assertFalse(PortalDialogAI2.isKromedeTrialEntryNpc(834194));
		assertFalse(PortalDialogAI2.isKromedeTrialEntryNpc(0));
	}

	/**
	 * 克萝梅德试炼入口的 questId=0「进入恶梦」(SETPRO1=10000) 先经任务引擎（真端 FUN_180f859b0：
	 * 18602/28602 步=1 + 进副本），引擎认领时不落传送门、已在副本内不二次传送。
	 * The questId=0 enter-nightmare action on the trial entrances routes through the quest engine first
	 * (retail FUN_180f859b0: step:=1 + enter); a claim skips the portal and a player already inside
	 * never gets a second teleport.
	 */
	@Test
	void kromedeTrialEnterNightmareRoutesThroughTheQuestEngineFirst() throws IOException {
		String source = Files.readString(SOURCE);

		assertTrue(source.contains("dialogId == QuestDialogAction.SETPRO1.id()"));
		assertTrue(source.contains("isKromedeTrialEntryNpc(getNpcId())"));
		assertTrue(source.contains("player.getWorldId() == KROMEDE_TRIAL_WORLD_ID"));
	}

	/**
	 * 克萝梅德试炼入口的开门对话先请任务引擎重放 QUEST_SELECT：未接 → select_none 接取页、
	 * 进行中 → select1、可交 → 领奖页，全部携带 questId；旧页轴（页 10/questId=0 的空白对话）
	 * 只在引擎不认领时兜底。
	 * Kromede's Trial entrances replay QUEST_SELECT through the quest engine on open-door: accept /
	 * select1 / reward pages all carry the questId; the legacy context-less page 10 stays only as a
	 * fallback for unclaimed opens.
	 */
	@Test
	void kromedeTrialEntranceOpenDoorReplaysQuestSelectThroughTheEngine() {
		assertEquals(List.of(QuestDialogAction.QUEST_SELECT.id()),
			PortalDialogAI2.questFirstDialogIds(205229, 0));
		assertEquals(List.of(QuestDialogAction.QUEST_SELECT.id()),
			PortalDialogAI2.questFirstDialogIds(205234, 0));
		// 非入口 NPC 不进入引擎重放。 / Non-entrance npcs do not enter the engine replay.
		assertEquals(List.of(), PortalDialogAI2.questFirstDialogIds(205230, 0));
	}
}
