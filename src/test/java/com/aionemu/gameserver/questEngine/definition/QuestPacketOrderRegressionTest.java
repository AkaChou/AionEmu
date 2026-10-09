package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.e2e.QuestE2ePacketValidator;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionOutcome;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定客户端可见任务页面必须在已提交的任务状态同步之后发送。
 * Locks client-visible quest pages to be sent after the committed quest state is synchronized.
 */
class QuestPacketOrderRegressionTest {
	@Test
	void quest1573SynchronizesRewardStateBeforeDefaultSuccessPage() throws Exception {
		assertRouteContract(1573, "v2", projection(QuestStatus.START, 2), "reward",
			projection(QuestStatus.REWARD, 2), 730025, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(new QuestAction.RemoveItem(182201735, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
	}

	@Test
	void quest2392SynchronizesEachSelectedItemBranchBeforeItsRewardWindow() throws Exception {
		assertItemRewardRoute(QuestDialogAction.SETPRO1, 182204159, "r1", 1,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertItemRewardRoute(QuestDialogAction.SETPRO2, 182204160, "r2", 2,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		assertItemRewardRoute(QuestDialogAction.SETPRO3, 182204161, "r3", 3,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW3);
	}

	@Test
	void quest2533SynchronizesRewardStateBeforeReportPage() throws Exception {
		assertRouteContract(2533, "v1", projection(QuestStatus.START, 1), "reward",
			projection(QuestStatus.REWARD, 1), 204801, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
	}

	@Test
	void quest10032SynchronizesConsumedItemBeforeSuccessPage() throws Exception {
		assertRouteContract(10032, "s7", projection(QuestStatus.START, 7), "s7",
			projection(QuestStatus.START, 7), 799503, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0,
			List.of(new QuestCondition.QuestVariableIs("var0", 7), new QuestCondition.HasItem(182215620, 1)),
			List.of(new QuestAction.RemoveItem(182215620, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())));
	}

	@Test
	void quest24153SynchronizesRewardStateBeforeRewardWindow() {
		// 24153 已退役（保留清单 owner=RETAIL_TABLE，SimpleHunt 车道，XML 只在 git 历史）：typed 满段
		// 节点（a1b1c1d1e1）随 XML 退场，同一不变式改由 native 面复核——五杀各推进一格（末杀
		// ADVANCE_WRITE → REWARD）并在击杀时即发 SM_QUEST_ACTION，随后交付 NPC 的报告动作才发奖励窗页 5。
		// Quest 24153 is retired (SimpleHunt lane), so the typed full node retires with the XML; the same
		// invariant is re-anchored natively: five kills advance the grid (the last one flips REWARD and
		// emits SM_QUEST_ACTION), and only the later report dialog sends reward page 5.
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertTrue(RetiredQuestIds.contains(24153));
		assertTrue(handler.routes(24153), "SimpleHunt native 车道必须路由 24153");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(24153).isPresent(),
			"退役后 typed 目录不得再持有 24153");
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(24153);
		assertEquals("DF3_NPC_Akigatan", row.acquiredNpcName(), "原版接取 NPC");
		assertEquals("DF3_NPC_Akigatan", row.rewardNpcName(), "原版交付 NPC");
		assertEquals(204787, handler.rewardNpc(24153), "交付 NPC = DF3_NPC_Akigatan");
		assertEquals(204784, NativeNpcNameResolver.instance().resolve("Delris").npcIds().getFirst(),
			"原版 talk_npc1 = Delris");

		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 43);
		NativeTalkFixture.add(player, 24153, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(player);
		int expectedVars = 0;
		for (int slot = 1; slot <= 5; slot++) {
			int npcId = NativeNpcNameResolver.instance()
				.resolveMonsterIds(row.killSlots().get(slot).monsters().getFirst()).getFirst();
			assertTrue(handler.onKill(player, npcId), "槽 " + slot + " 击杀必须推进网格");
			expectedVars |= row.killSlots().get(slot).count() << (6 * (slot - 1));
		}
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(24153).getStatus(),
			"末杀满段必须翻领奖态");
		assertEquals(expectedVars, player.getQuestStateList().getQuestState(24153).getQuestVars().getQuestVars(),
			"满段网格 = 五格各自计数（原版 6 位/槽打包）");

		// 不清队列：击杀时的状态包必须先于交付 NPC 报告动作的页面（同一条不变式）。
		// The kill-time state packets stay in the queue: they must precede the report page.
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 204787, 24153, 31)), "交付 NPC 报告动作");
		NativeTalkFixture.assertQuestActionBeforeDialogWindow(player);
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, 24153);
	}

	@Test
	void protocolLoopSendsCommittedStateBeforeEveryRepairedPage() throws Exception {
		assertProtocolPacketOrder(1573, "v2", QuestDialogAction.QUEST_SELECT.id(), null);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO1.id(), 0);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO2.id(), 0);
		assertProtocolPacketOrder(2392, "started", QuestDialogAction.SETPRO3.id(), 0);
		assertProtocolPacketOrder(2533, "v1", QuestDialogAction.QUEST_SELECT.id(), null);
		assertProtocolPacketOrder(10032, "s7", QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0);
		// 24153 已退役（SimpleHunt 车道）：typed 满段节点不存在，协议回环不可跑；同一条
		// 「已提交状态先于页面」不变式由 quest24153SynchronizesRewardStateBeforeRewardWindow
		// 在 native 包队列上复核。
		// Quest 24153 is retired (SimpleHunt lane): no typed full node remains, so the protocol loop
		// cannot run it; the sibling method re-checks the same invariant on the native packet queue.
	}

	private static void assertItemRewardRoute(QuestDialogAction action, int itemId, String target, int variable,
			QuestDialogPage page) throws Exception {
		assertRouteContract(2392, "started", projection(QuestStatus.START, 0), target,
			projection(QuestStatus.REWARD, variable), 798085, action, 0,
			List.of(new QuestCondition.HasItem(itemId, 1)), List.of(new QuestAction.RemoveItem(itemId, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertProtocolPacketOrder(int questId, String source, int dialogId,
			Integer priority) throws Exception {
		CompiledQuestDefinition definition = compiledDefinition(questId);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.filter(candidate -> Objects.equals(priority, candidate.priority()))
			.findFirst().orElseThrow();
		QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int objectId = runtime.expectedDialogTargetObjectId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				ClientActionOutcome outcome = protocol.dispatch(
					ClientActionRequest.dialog(questId, talk.npcId(), objectId, dialogId));
				assertTrue(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				QuestE2ePacketValidator.Result validation = QuestE2ePacketValidator.validate(
					definition, transition, objectId, outcome.packets());
				assertTrue(validation.valid(), () -> questId + ": " + validation);
				List<ServerPacketObservation.Type> packetTypes = outcome.packets().stream()
					.map(ServerPacketObservation::type).toList();
				int syncIndex = packetTypes.indexOf(ServerPacketObservation.Type.QUEST_ACTION);
				int pageIndex = packetTypes.indexOf(ServerPacketObservation.Type.DIALOG_WINDOW);
				assertTrue(syncIndex >= 0 && pageIndex > syncIndex,
					() -> questId + ": packet order=" + packetTypes);
			}
		}
	}

	private static void assertRouteContract(int questId, String source, NodeProjection sourceProjection,
			String target, NodeProjection targetProjection, int npcId, QuestDialogAction action, Integer priority,
			List<QuestCondition> conditions, List<QuestAction> actions, List<AfterCommitAction> afterCommit)
			throws Exception {
		QuestDefinition definition = definition(questId);
		QuestTransition transition = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.filter(candidate -> Objects.equals(priority, candidate.priority()))
			.findFirst().orElseThrow();

		assertEquals(source, transition.sourceNode());
		assertEquals(sourceProjection, node(definition, source).projection());
		assertEquals(new QuestEvent.TalkToNpc(npcId, action.id()), transition.event());
		assertEquals(priority, transition.priority());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(target, transition.targetNode());
		assertEquals(targetProjection, node(definition, target).projection());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static NodeProjection projection(QuestStatus status, int var0) {
		return new NodeProjection(status, Map.of("var0", var0));
	}

	private static QuestDefinition definition(int questId) throws Exception {
		return compiledDefinition(questId).definition();
	}

	private static CompiledQuestDefinition compiledDefinition(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
