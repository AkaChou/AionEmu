package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1192「贝尔特伦要塞的支援请求 / Verteron Reinforcements」的三段交付链。
 * Locks the three-step hand-over chain of quest 1192.
 * <p>Aion 5.8 客户端 quest_summary 声明了三行：极乐世界把书信交给拉比临托斯(203701)、
 * 贤者书库和科赛诺芬(203833)对话、回贝尔特伦要塞和斯帕塔洛斯(203098)对话，三条对话链各自以
 * {@code HACTION_SETPRO1}、{@code HACTION_SETPRO2}、{@code HACTION_SELECT_QUEST_REWARD} 收口。
 * 迁移后的定义只剩一个 {@code started(var0=0)} 进行中状态，把 SETPRO1 同时挂在 203701 与 203833 上并直接进 reward，
 * {@code SETPRO2}/{@code SELECT3_1}/{@code SELECT5} 完全没有路由：玩家跟拉比临托斯说完话就能跳过第 2、3 步直接领奖，
 * wiki 侧表现为三步共用同一条 {@code //quest set 1192 START 0}。
 * The Aion 5.8 client journal declares three rows (Lavirintos in Sanctum, Xenophon in the Library of the Sages,
 * and Spatalos back at Verteron Citadel), each backed by its own dialog chain ending in
 * {@code HACTION_SETPRO1}, {@code HACTION_SETPRO2} and {@code HACTION_SELECT_QUEST_REWARD}. The migrated definition
 * kept a single {@code started(var0=0)} progress state, hung SETPRO1 on both 203701 and 203833 straight into reward,
 * and left {@code SETPRO2}/{@code SELECT3_1}/{@code SELECT5} unrouted, so talking to Lavirintos skipped steps 2 and 3
 * and jumped to the reward window.</p>
 */
class Quest1192StepChainContractTest {
	private static final int SPATALOS = 203098;
	private static final int LAVIRINTOS = 203701;
	private static final int XENOPHON = 203833;
	private static final int REINFORCEMENT_REQUEST = 182200556;

	@Test
	void clientJournalRowsMapToOneProgressStateEach() throws Exception {
		QuestDefinition definition = definition().definition();
		// 客户端任务书三行各占一个 var0（QE-051：每一行都必须有 START/REWARD 状态），领奖行 = 第 3 行（var0=2）。
		assertEquals(List.of(
			"unaccepted:NONE:0",
			"started:START:0",
			"s1:START:1",
			"s2:START:2",
			"reward:REWARD:2",
			"complete:COMPLETE:2"),
			definition.nodes().stream()
				.map(node -> node.label() + ":" + node.projection().status() + ":"
					+ node.projection().variables().get("var0"))
				.toList());
	}

	@Test
	void eachStepAdvancesItsOwnStateAndNeverJumpsStraightToReward() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();

		// 步骤 1：把书信交给拉比临托斯 → s1（书信在交出时清空，存量为 0 的旧存档不得阻断）。
		QuestTransition stepOne = talk(definition, "started", "s1", LAVIRINTOS,
			QuestDialogAction.SETPRO1.id());
		assertEquals(List.of(new QuestAction.RemoveItem(REINFORCEMENT_REQUEST, QuestAction.RemoveItem.ALL)),
			stepOne.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), stepOne.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			talk(definition, "started", "started", LAVIRINTOS, QuestDialogAction.SELECT2_1.id()).afterCommit());

		// 步骤 2：跟科赛诺芬对话 → s2。
		QuestTransition stepTwo = talk(definition, "s1", "s2", XENOPHON,
			QuestDialogAction.SETPRO2.id());
		assertEquals(List.of(), stepTwo.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), stepTwo.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())),
			talk(definition, "s1", "s1", XENOPHON, QuestDialogAction.SELECT3_1.id()).afterCommit());

		// 步骤 3：回贝尔特伦要塞向斯帕塔洛斯报告 → reward（领奖行 var0=2）。
		QuestTransition stepThree = talk(definition, "s2", "reward", SPATALOS,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			stepThree.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			talk(definition, "s2", "s2", SPATALOS, QuestDialogAction.QUEST_SELECT.id()).afterCommit());

		// 回归防呆：SETPRO1/SETPRO2 是「推进到下一步」的动作，任何一条都不得直接落到 reward。
		assertTrue(definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
				.map(candidate -> (QuestEvent.TalkToNpc) candidate.event())
				.noneMatch(talk -> talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.SETPRO1.id()
						|| talk.dialogId() == QuestDialogAction.SETPRO2.id())),
			"SETPRO1/SETPRO2 must advance the step chain instead of claiming the reward");

		// 运行时：三跳依次落在 START/1、START/2、REWARD/2。
		assertNextRow(compiled, QuestStatus.START, 0, stepOne, QuestStatus.START, 1);
		assertNextRow(compiled, QuestStatus.START, 1, stepTwo, QuestStatus.START, 2);
		assertNextRow(compiled, QuestStatus.START, 2, stepThree, QuestStatus.REWARD, 2);
	}

	@Test
	void onlySpatalosClaimsTheRewardAndTheWorkItemStaysDeclared() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode())
				&& "complete".equals(candidate.targetNode()))
			.toList();
		assertFalse(completions.isEmpty(), "reward → complete 领奖路由必须存在");
		assertEquals(List.of(SPATALOS), completions.stream()
				.map(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId())
				.distinct().sorted().toList(),
			"客户端只在斯帕塔洛斯处结束任务，旧定义让 203701/203833 也能领奖");
		assertEquals(List.of(new QuestItemRequirement(REINFORCEMENT_REQUEST, 1)),
			definition.metadata().questWorkItems());
	}

	@Test
	void persistedRewardRowIsRepairedOnEnterWorld() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestTransition recovery = recoveryRoute(compiled.definition());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", 0)), recovery.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), recovery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit());
		assertNull(recovery.priority());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 0)), recovery.event(), recovery)
			.orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(2, unpack(compiled, plan).get("var0"));
	}

	private static void assertNextRow(CompiledQuestDefinition definition, QuestStatus status, int var0,
			QuestTransition transition, QuestStatus expectedStatus, int expectedVar0) {
		QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
			snapshot(definition, status, Map.of("var0", var0)), transition.event(), transition).orElseThrow();
		assertEquals(expectedStatus, plan.nextStatus());
		assertEquals(expectedVar0, unpack(definition, plan).get("var0"));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& new QuestEvent.TalkToNpc(npcId, action).equals(candidate.event()))
			.findFirst().orElseThrow(() -> new AssertionError(
				"missing route " + source + " -> " + target + " npc=" + npcId + " action=" + action));
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(), "quest 1192 reward recovery route");
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables),
			Map.of(REINFORCEMENT_REQUEST, 1), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest1192StepChainContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1192.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1192.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
