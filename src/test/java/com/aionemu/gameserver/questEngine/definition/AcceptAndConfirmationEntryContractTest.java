package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定两类接取入口合同：
 * 1. 区域/事件自动发放任务：NONE 态不得有任何对话路由——接取由入口发放完成。1877 已由真端
 *    DataDriven PVP 网格接管（DD_PVP_GRID），发放入口从 EnterZone 升级为进区域的 SystemGrant，
 *    落在计数网格首节点 a0。
 * 2. check 族确认页：上交成功分支显示 CHECK_USER_ITEM_OK，确认按钮关闭对话或打开奖励窗口。
 * Locks two accept-entry contracts:
 * 1. Zone/event auto-start quests keep NONE free of dialog routes - the entry route owns the
 *    acceptance. 1877 is retail DataDriven PVP grid now (DD_PVP_GRID): the entry route was
 *    upgraded from EnterZone to the area-entry SystemGrant landing on the first grid node a0.
 * 2. Check-family confirmation pages: the hand-over success branch shows CHECK_USER_ITEM_OK and
 *    the confirmation button closes the dialog or opens the reward window.
 */
class AcceptAndConfirmationEntryContractTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void areaAutoStartQuestsKeepUnacceptedFreeOfDialogRoutes() throws Exception {
		// 已验收合同的 DD 延续：1877 在 NONE 态依旧没有任何对话路由——接取由进区域的
		// SystemGrant 完成（真端 PVP 网格，区域发放落 a0，StartEligible 门禁 + 可见性刷新）。
		// DD continuation of the accepted contract: 1877 keeps NONE free of dialog routes - the
		// area-entry SystemGrant owns acceptance (retail PVP grid, the grant lands on a0 gated
		// by StartEligible with a visibility refresh).
		// TEMP-VERIFY(view): 并行 SimpleTalk 批次落定前生产覆盖门不可用，用宽松 overlay 验证本断言。
		QuestDefinition definition = verificationView().find(1877).orElseThrow().definition();
		assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.sourceNode() != null && Objects.equals(transition.sourceNode(), "unaccepted")
					&& transition.event() instanceof QuestEvent.TalkToNpc),
			"quest 1877 unaccepted must stay free of dialog routes");
		QuestTransition start = definition.transitions().stream()
			.filter(transition -> transition.sourceNode() != null
				&& Objects.equals(transition.sourceNode(), "unaccepted")
				&& transition.event() instanceof QuestEvent.SystemGrant)
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.StartEligible()), start.conditions(),
			"quest 1877 area grant must stay start-eligible gated");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			start.afterCommit(), "quest 1877 area grant refreshes visibility");
		// PVP 计数网格：a0..aN 逐态推进，N 由领奖投影推导（真端 PVP 计数 = a0..aN）。
		// PVP counter grid: a0..aN advance state by state; N derives from the reward projection.
		int required = definition.nodes().stream()
			.filter(node -> node.label().equals("reward"))
			.findFirst().orElseThrow().projection().variables().get("var0");
		assertTrue(required > 0, () -> "quest 1877 reward projection must carry the counter " + required);
		assertEquals("a0", start.targetNode(), "quest 1877 area grant owns NONE -> first grid node");
		assertEquals(QuestStatus.NONE, projection(definition, "unaccepted").status());
		for (int kills = 0; kills <= required; kills++) {
			final int state = kills;
			assertEquals(QuestStatus.START, projection(definition, "a" + kills).status(),
				() -> "quest 1877 grid node a" + state + " stays START");
			assertEquals(Map.of("var0", state), projection(definition, "a" + kills).variables(),
				() -> "quest 1877 grid node a" + state + " projects the kill counter");
		}
		assertEquals(QuestStatus.REWARD, projection(definition, "reward").status());
		assertEquals(QuestStatus.COMPLETE, projection(definition, "complete").status());
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

		// 15010：NPC_REPORT 检查对——1009 命中物品进 REWARD+奖励窗，未命中显示 fail 页。
		// 15010: the explicit 1009 check pair - with the items REWARD + reward window, without
		// them the fail page.
		QuestDefinition report = compile(15010);
		List<QuestTransition> claims = report.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 804700 && talk.dialogId() == 1009
				&& "started".equals(transition.sourceNode()))
			.toList();
		assertEquals(2, claims.size(), "quest 15010 check branch count");
		QuestTransition hit = claims.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition miss = claims.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();
		assertEquals("reward", hit.targetNode(), "quest 15010 hit target");
		assertEquals(List.of(
				new QuestCondition.HasItem(182215664, 5),
				new QuestCondition.HasItem(182215665, 3)), hit.conditions(),
			"quest 15010 hit conditions");
		assertEquals(List.of(
				new QuestAction.RemoveItem(182215664, 5),
				new QuestAction.RemoveItem(182215665, 3)), hit.actions(),
			"quest 15010 hit removes the items");
		assertTrue(hit.afterCommit().contains(
			new AfterCommitAction.ShowQuestDialog(5)), "quest 15010 hit opens reward window 1");
		assertEquals("started", miss.targetNode(), "quest 15010 miss target");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), miss.afterCommit(),
			"quest 15010 miss shows check_user_item_fail");
		QuestTransition missClose = talkRoute(report, "started", 804700, 1008);
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), missClose.afterCommit(),
			"quest 15010 fail-page button must close");
	}

	// TEMP-VERIFY(view): 并行批次落定前的宽松生产视图（XML 目录 + 真端驱动，跳过覆盖门）。
	private static final java.util.concurrent.atomic.AtomicReference<QuestCatalog> VIEW =
		new java.util.concurrent.atomic.AtomicReference<>();

	private static QuestCatalog verificationView() {
		return VIEW.updateAndGet(current -> current != null ? current
			: RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
				AcceptAndConfirmationEntryContractTest.class.getClassLoader())));
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static NodeProjection projection(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> node.label().equals(label)).findFirst().orElseThrow().projection();
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
