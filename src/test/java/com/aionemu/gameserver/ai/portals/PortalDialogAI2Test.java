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
	 * 克萝梅德试炼入口的 questId=0「进入恶梦」(SETPRO1=10000) 先经任务引擎（原版 FUN_180f859b0：
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

	/**
	 * 未认领任务动作的分类（2026-10-08 实机 19640）：奖励窗确认动作 8 是按钮而不是页——回显会让
	 * 客户端按动作 id 加载 {@code Quest_Q19640.html} 的页 8 并 load fail，必须关窗；声明过的子页动作
	 * 按任务页回显（带 questId，1115 声明 1353=select2_1），未声明的子页按 NPC 对话平面回显（不带 questId，
	 * DialogService 守卫的既有语义）。
	 * The unclaimed-action classification (live 19640): the reward-window button 8 must close the dialog
	 * instead of being echoed as a page; declared sub-pages echo with the quest context (1115 declares
	 * 1353) and undeclared sub-pages echo on the plain NPC dialog plane (the DialogService guard shape).
	 */
	@Test
	void unclaimedQuestActionsClassifyIntoEchoOrClose() {
		assertEquals(PortalDialogAI2.UnclaimedReply.CLOSE, PortalDialogAI2.unclaimedReply(19640, 8),
			"奖励窗确认动作 8 不是页 / action 8 is a button, not a page");
		assertEquals(PortalDialogAI2.UnclaimedReply.CLOSE, PortalDialogAI2.unclaimedReply(19640, 31),
			"行选动作不是页导航 / row selection is not page navigation");
		assertEquals(PortalDialogAI2.UnclaimedReply.CLOSE, PortalDialogAI2.unclaimedReply(19640, 1009),
			"报告动作不是页导航 / the report action is not page navigation");
		assertEquals(PortalDialogAI2.UnclaimedReply.CLOSE, PortalDialogAI2.unclaimedReply(19640, 4762),
			"顶层页 id（select_none）不是子页动作 / a top-level page id is not a sub-page action");
		assertEquals(PortalDialogAI2.UnclaimedReply.DECLARED_SUB_PAGE,
			PortalDialogAI2.unclaimedReply(1115, 1353),
			"声明的子页带 questId 回显 / declared sub-pages echo with the quest context");
		assertEquals(PortalDialogAI2.UnclaimedReply.PLAIN_SUB_PAGE,
			PortalDialogAI2.unclaimedReply(1115, 1012),
			"未声明子页按 NPC 对话平面回显 / undeclared sub-pages echo on the plain dialog plane");
	}

	/**
	 * 两个传送门类 AI 共用同一分类判据：{@code onDialogSelect} 未认领分支必须走
	 * {@code unclaimedReply}（PortalDialogAI2 本类判定；Specialize01PortalAI2 委托），
	 * 按钮动作一律关窗、绝不回显动作 id。
	 * Both portal AIs must share the classification: the unclaimed branch reads {@code unclaimedReply},
	 * and button actions close the dialog instead of being echoed as pages.
	 */
	@Test
	void bothPortalAisRouteUnclaimedActionsThroughTheSharedClassification() throws IOException {
		assertTrue(Files.readString(SOURCE).contains("unclaimedReply(questId, dialogId)"),
			"PortalDialogAI2 未认领分支必须走分类判据 / the unclaimed branch reads the classification");
		String specialize = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/portals/Specialize01PortalAI2.java"));
		assertTrue(specialize.contains("PortalDialogAI2.unclaimedReply("),
			"Specialize01PortalAI2 必须委托同一判据 / the sibling delegates to the same classification");
		assertTrue(specialize.contains("DialogService.closeDialog("),
			"按钮动作必须关窗 / button actions must close the dialog");
	}
}
