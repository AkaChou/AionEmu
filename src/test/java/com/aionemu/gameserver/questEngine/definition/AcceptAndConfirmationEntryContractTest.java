package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定两类接取入口合同：
 * 1. 区域/事件自动发放任务：NONE 态不得有任何对话路由——接取由入口发放完成。1877 已退役
 *    （retention: RETAIL_TABLE，basis DD_PVP_GRID），IR 不复存在：断言重锚到 native 面——
 *    运行时不得为它注册对话接取面，PVP 计数网格落在运行时 pvp 兴趣面（发放/登记面由
 *    {@code RetailEnterAreaZoneRegistrationGateTest} 与 DD 行矩阵门承担）。
 * 2. check 族确认页：上交成功分支显示 CHECK_USER_ITEM_OK，确认按钮关闭对话或打开奖励窗口（1636，
 *    XML 保留）。原 15010 检查对随其退役退场（retention: RETAIL_TABLE，basis DD_HANDIN_CANONICAL；
 *    DD 交付面语义由运行时门承担）。
 * Locks two accept-entry contracts. 1877 is retired (retention: RETAIL_TABLE, basis DD_PVP_GRID):
 * its IR is gone, so the assertions re-anchor to the native runtime — no talk-acquire face may be
 * registered for it and the pvp counter grid lives on the runtime pvp interest face. The 15010 check
 * pair retired with its quest; 1636 stays the XML-retained representative of the check-page contract.
 */
class AcceptAndConfirmationEntryContractTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests");

	@Test
	void areaAutoStartQuestsKeepUnacceptedFreeOfDialogRoutes() {
		// 已验收合同的 native 延续（2026-10-05 重锚；原 lax overlay 视图随 P7 步 f 退场）：
		// 1877 退役后无 IR 对话路由可言（结构性），且运行时兴趣面不得为它注册任何对话接取
		// （NONE 态玩家不可能经 NPC 对话接取）；接取由进区域的 SystemGrant 完成，计数走 PVP 网格。
		// Native continuation of the accepted contract (re-anchored 2026-10-05; the old lax overlay
		// view retired with P7 step f): with no IR left a dialog route on NONE is structurally
		// impossible, and the runtime must register no talk-acquire face for it.
		assertTrue(RetiredQuestIds.contains(1877), "quest 1877 must be retail-table owned");
		DataDrivenNativeRuntime runtime = DataDrivenNativeRuntime.instance();
		assertTrue(runtime.owns(1877), "quest 1877 must be owned by the DataDriven runtime");
		assertTrue(runtime.acquireTalkInterests().values().stream().flatMap(List::stream)
			.noneMatch(questId -> questId == 1877),
			"quest 1877 unaccepted must stay free of talk-acquire faces");
		assertFalse(runtime.pvpSteps().getOrDefault(1877, List.of()).isEmpty(),
			"quest 1877 pvp counter grid must live on the runtime pvp interest face");
	}

	@Test
	void checkHandOverShowsTheClientConfirmationPageThenClosesOrClaims() throws Exception {
		// 1636：39 成功分支 v1->v2 显示确认页，v2 的 FINISH_DIALOG 关闭，领奖经 REWARD 态入口。
		// 1636: the 39 success branch v1->v2 shows the confirmation page, v2 FINISH closes, and
		// the reward is claimed through the REWARD-state entry.
		QuestDefinition definition = compile(1636);
		QuestTransition success = definition.transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "v1")
				&& transition.targetNode().equals("v2")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203792 && talk.dialogId() == 39)
			.findFirst().orElseThrow();
		assertEquals(List.of(
				new QuestCondition.HasItem(182201786, 1),
				new QuestCondition.HasItem(152020034, 1),
				new QuestCondition.HasItem(152020091, 1),
				new QuestCondition.HasItem(169400060, 1)), success.conditions(),
			"quest 1636 hand-over conditions");
		assertEquals(List.of(
				new QuestAction.RemoveItem(182201786, 1),
				new QuestAction.RemoveItem(152020034, 1),
				new QuestAction.RemoveItem(152020091, 1),
				new QuestAction.RemoveItem(169400060, 1)), success.actions(),
			"quest 1636 hand-over removes the spirit and crafting materials");
		assertTrue(success.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(10000)),
			"quest 1636 hand-over must show check_user_item_ok");
		QuestTransition confirm = talkRoute(definition, "v2", 203792, 1008);
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), confirm.afterCommit(),
			"quest 1636 confirmation button must close");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203792
					&& talk.dialogId() == 1009 && "v2".equals(transition.sourceNode())),
			"quest 1636 reward claim belongs to the REWARD-state preview, not the ok page");
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static QuestTransition talkRoute(QuestDefinition definition, String source, int npcId,
			int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}
}
