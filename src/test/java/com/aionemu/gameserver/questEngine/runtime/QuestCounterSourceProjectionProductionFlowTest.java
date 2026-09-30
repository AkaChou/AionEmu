package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionOutcome;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证共享 START 节点不会把实时击杀计数锁死或重置，并锁定最后一次击杀和报告的生产流。
 * Verifies that shared START nodes neither lock nor reset live kill counters and preserves final-kill/report flows.
 */
class QuestCounterSourceProjectionProductionFlowTest {
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> SUB_BOSSES = Set.of(857450, 857452, 857454, 857456, 857458, 857459);
	private static final Set<Integer> CAPTAINS = Set.of(219256, 219259, 219267, 219261, 219271);
	private static final Set<Integer> ADJUTANTS = Set.of(219257, 219258, 219268, 219260, 219262);
	private static final Set<Integer> GI_GUARDIANS = Set.of(219286, 243852);

	@Test
	void archivesDualCounterQuestsWalkTheSequentialSectionChain() throws Exception {
		// 26802/16802 是同构的天魔档案馆顺序链：真端表段序 = 30 图书管理员（段 1）→ 2 sub-boss
		// （段 2），客户端 quest_monster.csv 的链式 SECTION 门控决定乱序不计数——段 1 进行中
		// Boss 击杀不产生任何计划，段 1 打满后 Boss 才逐只推进，满段经 1009 领奖。
		// 26802/16802 are twin Archives sequential chains: the retail stage order is 30 librarians
		// (stage 1) then 2 sub-bosses (stage 2); the client's chained SECTION gates make out-of-order
		// kills never count — boss kills produce no plan during stage 1 and the full chain reports.
		for (int questId : List.of(26802, 16802)) {
			CompiledQuestDefinition definition = load(questId);
			assertEquals(Map.of("var0", 0, "var1", 0), node(definition, "a0b0").projection().variables());
			assertSequentialOrder(definition, true);
			assertSequentialOrder(definition, false);
		}
	}

	@Test
	void quests30603And30613PreserveFourIndependentRetailCounters() throws Exception {
		assertFourCounterMonsterHunt(load(30603), 800325);
		assertFourCounterMonsterHunt(load(30613), 800327);
	}

	private static void assertSequentialOrder(CompiledQuestDefinition definition, boolean negativeProbeFirst)
			throws Exception {
		// 系统发放接取（EnterArea）：SystemGrant 边引导进链首 a0b0。
		// System-grant acquire (EnterArea): the SystemGrant edge leads into the first chain node.
		QuestTransition grant = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.SystemGrant)
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(grant);
			runtime.dispatchPrepared();
			assertEquals(QuestStatus.START, runtime.state().status());
			// 负例只属于段 1 进行中（a0b0）；a30b0 起 Boss 已是当前段、击杀合法计数。
			// The negative probe belongs to stage 1 only (a0b0); from a30b0 on, bosses are the
			// current stage and their kills legitimately count.
			if (negativeProbeFirst) {
				assertFalse(runtime.dispatchWorld(new QuestEvent.KillNpc(857450)).handled());
			}
			dispatchKills(runtime, 220306, 30);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 30, "var1", 0), variables(definition, runtime));
			dispatchKills(runtime, 857450, 2);
			// 满段仍是 START：真端把领奖入口放在报告 NPC 的 QUEST_SELECT 交付之后（P0-2 规范形）。
			// The full chain stays START: retail keeps reward entry behind the report NPC's
			// QUEST_SELECT delivery (canonical since P0-2).
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 30, "var1", 2), variables(definition, runtime));
			// P0-2 顺序链规范形：满段 QUEST_SELECT 直翻领奖（1009 中转删除）。
			// Canonical sequential delivery: the full node's QUEST_SELECT flips reward (no 1009 hop).
			QuestTransition finish = definition.definition().transitions().stream()
				.filter(candidate -> "a30b2".equals(candidate.sourceNode())
					&& "reward".equals(candidate.targetNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
				.findFirst().orElseThrow();
			assertTrue(runtime.dispatchWorld(finish.event()).handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
		}
	}

	private static void assertFourCounterMonsterHunt(CompiledQuestDefinition definition, int reportNpcId)
			throws Exception {
		assertEquals(Map.of(), node(definition, "started").projection().variables());
		assertEquals(new NodeProjection(QuestStatus.REWARD,
			Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1)),
			node(definition, "reward").projection());
		assertCounter(definition, CAPTAINS, "var0", 9);
		assertCounter(definition, ADJUTANTS, "var1", 9);
		assertCounter(definition, Set.of(219255), "var2", 1);
		assertCounter(definition, GI_GUARDIANS, "var3", 1);

		QuestTransition report = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("started"))
			.filter(candidate -> candidate.targetNode().equals("reward"))
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestCondition.VariableAtLeast("var0", 9),
			new QuestCondition.VariableAtLeast("var1", 9),
			new QuestCondition.VariableAtLeast("var2", 1),
			new QuestCondition.VariableAtLeast("var3", 1)), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition firstCounter = killRoute(definition, CAPTAINS, 1);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(firstCounter);
			dispatchKills(runtime, 243852, 1);
			dispatchKills(runtime, 219255, 1);
			dispatchKills(runtime, 219257, 9);
			dispatchKills(runtime, 219256, 9);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
			assertFalse(runtime.dispatchWorld(new QuestEvent.KillNpc(219256)).handled());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
		}

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(report);
			ClientActionOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 9, "var1", 9, "var2", 1, "var3", 1),
				variables(definition, runtime));
		}
	}

	private static void assertCounter(CompiledQuestDefinition definition, Set<Integer> npcIds, String field,
			int required) {
		QuestTransition transition = killRoute(definition, npcIds, 1);
		assertEquals("started", transition.sourceNode());
		assertEquals("started", transition.targetNode());
		assertEquals(List.of(new QuestCondition.VariableBelow(field, required)), transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, Set<Integer> npcIds,
			int priority) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.KillNpcSet(npcIds)))
			.filter(candidate -> Objects.equals(candidate.priority(), priority))
			.findFirst().orElseThrow();
	}

	private static void dispatchKills(QuestE2eRuntime runtime, int npcId, int count) {
		for (int index = 0; index < count; index++) {
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(npcId)).handled(),
				"kill " + npcId + " was not handled at index " + index);
		}
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	/** 30603/30613 仍由 XML 拥有；26802/16802 走生产驱动（XML 已退役）。 /
	 * 30603/30613 remain XML-owned; 26802/16802 load through the production driver. */
	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest/definitions/quests/" + questId + ".xml";
		InputStream input = QuestCounterSourceProjectionProductionFlowTest.class.getResourceAsStream(resource);
		if (input == null) {
			return ProductionQuestDefinitions.definition(questId);
		}
		try (input) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
