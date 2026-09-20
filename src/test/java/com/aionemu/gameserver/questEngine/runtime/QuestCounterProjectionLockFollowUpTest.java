package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 COUNTER_PROJECTION_LOCK 后续批次（4711/30710/49702）的完整行为合同：
 * START 源节点不投影实时计数字段，计数与步骤链由 transition 条件驱动，最后一击或交互进入 REWARD。
 * 第二批（15321/25608/27510）同型：击杀自环阶段节点只固定阶段位段 var0，
 * 计数字段 var1 由击杀转换拥有；把计数钉进 source 投影会让第一只怪之后全部 NO_MATCH。
 * Locks the full behavior contract of the follow-up COUNTER_PROJECTION_LOCK batch (4711/30710/49702):
 * the START source node projects no live counter field; counters and step chains are driven by
 * transition conditions and the final kill or interaction enters REWARD.
 */
class QuestCounterProjectionLockFollowUpTest {

	@Test
	void quest49702CountsSixKillsThroughUnlockedStartNode() throws Exception {
		CompiledQuestDefinition definition = load(49702);
		assertEquals(Map.of(), node(definition, "started").projection().variables());

		QuestTransition counting = killRoute(definition, 701568, 1);
		assertEquals("started", counting.sourceNode());
		assertEquals("started", counting.targetNode());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 6)), counting.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), counting.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			counting.afterCommit());

		QuestTransition finishing = killRoute(definition, 701568, 0);
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 6)), finishing.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), finishing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finishing.afterCommit());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			for (int index = 1; index <= 5; index++) {
				assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(701568)).handled(),
					"kill at index " + index + " was not handled");
				assertEquals(QuestStatus.START, runtime.state().status());
				assertEquals(Map.of("var0", index), variables(definition, runtime));
			}
			runtime.prepare(finishing);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 6), variables(definition, runtime));
		}
	}

	@Test
	void quest30710UseObjectEntersRewardWithoutStartProjectionLock() throws Exception {
		CompiledQuestDefinition definition = load(30710);
		assertEquals(Map.of(), node(definition, "started").projection().variables());

		QuestTransition useObject = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(701498, QuestDialogAction.USE_OBJECT.id())))
			.findFirst().orElseThrow();
		assertEquals("started", useObject.sourceNode());
		assertEquals("reward", useObject.targetNode());
		assertEquals(List.of(), useObject.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), useObject.actions());
		assertEquals(3, useObject.afterCommit().size());
		assertTrue(useObject.afterCommit().get(0) instanceof AfterCommitAction.SpawnNpc spawn
			&& "gabelline".equals(spawn.slot()) && spawn.templateId() == 800457);
		assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			useObject.afterCommit().get(1));
		assertEquals(new AfterCommitAction.CloseDialog(), useObject.afterCommit().get(2));

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(useObject);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 1), variables(definition, runtime));
		}
	}

	@Test
	void quest4711RetailStepChainReachesFinalKill() throws Exception {
		CompiledQuestDefinition definition = load(4711);
		assertEquals(Map.of(), node(definition, "started").projection().variables());

		QuestTransition talkStep = dialogRoute(definition, 279042, QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), talkStep.conditions());
		assertEquals(List.of(), talkStep.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			talkStep.afterCommit());

		QuestTransition firstHandoff = dialogRoute(definition, 279042, QuestDialogAction.SETPRO1);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), firstHandoff.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), firstHandoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), firstHandoff.afterCommit());

		QuestTransition secondHandoff = dialogRoute(definition, 730196, QuestDialogAction.SETPRO2);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), secondHandoff.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), secondHandoff.actions());

		QuestTransition finalKill = killRoute(definition, 214823, null);
		assertEquals("started", finalKill.sourceNode());
		assertEquals("reward", finalKill.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), finalKill.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finalKill.afterCommit());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(talkStep);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", 0), variables(definition, runtime));
			runtime.prepare(firstHandoff);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", 1), variables(definition, runtime));
			runtime.prepare(secondHandoff);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(Map.of("var0", 2), variables(definition, runtime));
			runtime.prepare(finalKill);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 0)),
				node(definition, "reward").projection());
		}
	}

	/**
	 * 15321：s1 阶段的 30 只怪必须逐只计入并推进到 s2。
	 * 修复前 s1 投影 var1=0，第一只怪把 var1 写成 1 之后 source 不再匹配，
	 * 之后每次击杀都 NO_MATCH：玩家看到"击杀只有第一只算，任务不往下"。
	 */
	@Test
	void quest15321CountsEveryKillOfTheShoeHuntStage() throws Exception {
		CompiledQuestDefinition definition = load(15321);
		assertEquals(Map.of("var0", 1), node(definition, "s1").projection().variables(),
			"the kill stage must not pin the live counter var1");

		QuestTransition counting = killRoute(definition, 235829, 1);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			for (int index = 1; index <= 30; index++) {
				assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(235829)).handled(),
					"kill at index " + index + " was not handled");
			}
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 2, "var1", 0), variables(definition, runtime));
		}
	}

	/** 25608：step2 的 10 只 241235 必须整段计入后才推进到 step3。 */
	@Test
	void quest25608CountsEveryKillBeforeTheReport() throws Exception {
		CompiledQuestDefinition definition = load(25608);
		assertEquals(Map.of("var0", 2), node(definition, "step2").projection().variables(),
			"the kill stage must not pin the live counter var1");

		QuestTransition counting = killRoute(definition, 241235, 1);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			for (int index = 1; index <= 10; index++) {
				assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(241235)).handled(),
					"kill at index " + index + " was not handled");
			}
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 3, "var1", 0), variables(definition, runtime));
		}
	}

	/**
	 * 27510：s3 阶段并行计数精英怪（var1 0..10）与无名 boss（var2 开关），
	 * 两种击杀都必须计入并进入 s4。
	 */
	@Test
	void quest27510CountsBothEliteFamiliesBeforeTheReport() throws Exception {
		CompiledQuestDefinition definition = load(27510);
		assertEquals(Map.of("var0", 3), node(definition, "s3").projection().variables(),
			"the counter stage must not pin the live kill counters");

		QuestTransition counting = definition.definition().transitions().stream()
			.filter(candidate -> "s3".equals(candidate.sourceNode())
				&& "s3".equals(candidate.targetNode()))
			.filter(candidate -> Integer.valueOf(2).equals(candidate.priority()))
			.findFirst().orElseThrow();
		int countedNpc = lowestNpcId(counting);
		int flagNpc = definition.definition().transitions().stream()
			.filter(candidate -> "s3".equals(candidate.sourceNode())
				&& "s3".equals(candidate.targetNode()))
			.filter(candidate -> Integer.valueOf(1).equals(candidate.priority()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet npcIds
				&& !npcIds.npcIds().contains(countedNpc))
			.map(QuestCounterProjectionLockFollowUpTest::lowestNpcId)
			.findFirst().orElseThrow();

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			for (int index = 1; index <= 10; index++) {
				assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(countedNpc)).handled(),
					"elite kill at index " + index + " was not handled");
			}
			assertEquals(Map.of("var0", 3, "var1", 10, "var2", 0), variables(definition, runtime));
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(flagNpc)).handled(),
				"the nameless boss kill was not handled");
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 4, "var1", 0, "var2", 0), variables(definition, runtime));
		}
	}

	private static int lowestNpcId(QuestTransition transition) {
		return ((QuestEvent.KillNpcSet) transition.event()).npcIds().stream()
			.mapToInt(Integer::intValue).min().orElseThrow();
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, int npcId, Integer priority) {
		return definition.definition().transitions().stream()
			.filter(candidate -> switch (candidate.event()) {
				case QuestEvent.KillNpc(int id) -> id == npcId;
				case QuestEvent.KillNpcSet(Set<Integer> ids) -> ids.contains(npcId);
				default -> false;
			})
			.filter(candidate -> priority == null || candidate.priority() != null
				&& priority.equals(candidate.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition dialogRoute(CompiledQuestDefinition definition, int npcId,
			QuestDialogAction action) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestCounterProjectionLockFollowUpTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
