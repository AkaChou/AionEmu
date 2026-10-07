package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QE-054 步号轴收口：锁定领奖行批次误抬的 40 个任务——reward 投影 = 真端/legacy 推进值，
 * 批次坏档由反转后的无 source enter-world 边回滚；不再随客户端末行索引漂移。
 * <p>
 * 判定依据（同 1361/11006/24021 根因）：真端 {@code 0x100} 状态推进不写轴（进 REWARD 轴保持玩法末值），
 * 客户端 REWARD 态按自身 [{%N}] 行门槛显示报告行；「客户端末行是领奖行 ⇒ 投影抬到末行索引」
 * 对玩法步后进 REWARD 的任务系统性不成立。逐任务取证见
 * {@code .agents/summary/quest-step-axis-fullscan/}。
 * <p>
 * Locks the QE-054 step-axis close-out for the 40 quests mislifted by the reward-row batch: the
 * reward projection equals the retail/legacy progress value and batch-corrupted saves are rolled
 * back by the reversed source-less enter-world edge. Retail {@code 0x100} state advances never
 * touch the axis, so the client keeps showing the report row through its own [{%N}] gate.
 */
class RewardRowProjectionRegressionTest {

	/** (questId, 批次误抬值, 权威值, 批次前投影是否为 0——旧档直修边目标同权威值) */
	// (questId, batch-mislifted value, authoritative value, whether the pre-batch projection was 0)
	record Row(int questId, int batchRow, int authRow, boolean legacyZeroProjection) {
	}

	static Stream<Row> projections() {
		return Stream.of(
			new Row(1626, 7, 6, false), new Row(1636, 4, 3, false), new Row(2122, 2, 1, false),
			new Row(2208, 2, 1, false), new Row(2284, 3, 2, false), new Row(2333, 3, 2, false),
			new Row(2436, 2, 1, false), new Row(2620, 2, 1, false), new Row(3056, 2, 1, false),
			new Row(1319, 8, 7, true), new Row(1900, 4, 3, true), new Row(3082, 3, 2, false),
			new Row(3200, 4, 3, false), new Row(3721, 3, 2, false), new Row(4038, 3, 2, true),
			new Row(4502, 3, 2, false), new Row(4721, 3, 2, false), new Row(4939, 5, 4, true),
			new Row(4943, 4, 3, true), new Row(11116, 3, 2, true), new Row(11076, 4, 3, false),
			new Row(14046, 7, 6, false), new Row(14051, 4, 3, false), new Row(14153, 6, 5, false),
			new Row(10530, 9, 8, false), new Row(20530, 9, 8, false), new Row(21114, 5, 4, false),
			new Row(24022, 8, 7, false), new Row(24023, 4, 3, false), new Row(24024, 5, 4, false),
			new Row(24025, 4, 3, false), new Row(24030, 9, 8, false), new Row(24046, 7, 6, false),
			new Row(24051, 6, 5, false), new Row(30111, 2, 1, false), new Row(30227, 3, 2, false),
			new Row(30327, 3, 2, false), new Row(30217, 3, 2, false), new Row(30317, 3, 2, false),
			new Row(1921, 4, 3, false),
			/* 第二批（triage 全量扫描候选，2026-10-07 同日收口）：
			 * 3711/4711 legacy defaultOnKillEvent(214823,2,true) 落 2（三值自愈边 + 回滚）；
			 * 11031/11032/11033 legacy useQuestItem(2,3,true) 落 2（原无自愈边，补回滚）；
			 * 18208/28208/18209/28209 legacy 击杀链后 var0=1 + setStatus(REWARD)（范围边 + 回滚）；
			 * 28602 legacy defaultOnKillEvent(3,true) 落 3、真端 {1,2,3}。 */
			/* Second wave (full-scan triage candidates, closed the same day): 3711/4711 kill event
			 * persisted 2 (triple value edges + rollback); 11031-11033 item use persisted 2 (rollback
			 * edge added where none existed); the 18208 family left var0=1 before REWARD (range edges
			 * + rollback); 28602 kill event persisted 3 with retail set {1,2,3}. */
			new Row(3711, 3, 2, true), new Row(4711, 3, 2, true), new Row(11031, 3, 2, false),
			new Row(11032, 3, 2, false), new Row(11033, 3, 2, false), new Row(18208, 2, 1, false),
			new Row(28208, 2, 1, false), new Row(18209, 2, 1, false), new Row(28209, 2, 1, false),
			new Row(28602, 4, 3, false),
			/* 镜像对批次（MirrorPairRewardRowContractTest 第二批领奖行修复）同根因收口：
			 * 10110/20110 真端 {2,3,5} 无 6；14052/14026/24026 legacy setStatus(REWARD) 不写 var
			 * 落 4；18602 真端 {1,2,3} 无 4（镜像 28602）。 */
			/* Mirror-pair batch (the second reward-row repair wave), same root cause: 10110/20110
			 * retail {2,3,5} without 6; 14052/14026/24026 entered REWARD without touching var0
			 * (kept 4); 18602 retail {1,2,3} without 4 (mirror of 28602). */
			new Row(10110, 6, 5, false), new Row(20110, 6, 5, false), new Row(14052, 5, 4, false),
			new Row(14026, 5, 4, false), new Row(24026, 5, 4, false), new Row(18602, 4, 3, false));
	}

	@ParameterizedTest
	@MethodSource("projections")
	void rewardProjectionKeepsTheRetailProgressValue(Row row) throws Exception {
		QuestDefinition definition = load(row.questId()).definition();
		QuestNode reward = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, reward.projection().status());
		assertEquals(row.authRow(), reward.projection().variables().get("var0"),
			() -> "quest " + row.questId() + " reward projection must stay on the retail progress value");
	}

	@ParameterizedTest
	@MethodSource("projections")
	void batchCorruptedSavesRollBackToTheRetailValue(Row row) throws Exception {
		CompiledQuestDefinition compiled = load(row.questId());
		QuestTransition rollback = recoveryRoute(compiled.definition(), row.batchRow());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", row.batchRow())), rollback.conditions(),
			() -> "quest " + row.questId() + " rollback conditions");
		assertEquals(List.of(new QuestAction.SetVariable("var0", row.authRow())), rollback.actions(),
			() -> "quest " + row.questId() + " rollback actions");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), rollback.afterCommit());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, row.batchRow()), rollback.event(), rollback).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(row.authRow(), unpack(compiled, plan).get("var0"),
			() -> "quest " + row.questId() + " repaired journal row");
	}

	@ParameterizedTest
	@MethodSource("projections")
	void legacyZeroSavesAreHealedStraightToTheRetailValue(Row row) throws Exception {
		CompiledQuestDefinition compiled = load(row.questId());
		List<QuestTransition> recoveries = recoveryRoutes(compiled.definition());
		if (row.legacyZeroProjection()) {
			QuestTransition heal = recoveryRoute(compiled.definition(), 0);
			assertEquals(List.of(new QuestAction.SetVariable("var0", row.authRow())), heal.actions(),
				() -> "quest " + row.questId() + " legacy zero-save healing actions");
		} else {
			assertTrue(recoveries.stream().noneMatch(route -> route.conditions().contains(
				new QuestCondition.QuestVariableIs("var0", 0))),
				() -> "quest " + row.questId() + " must not heal zero saves");
		}
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = RewardRowProjectionRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			assertNotNull(input, "missing production quest " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static List<QuestTransition> recoveryRoutes(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition, int fromValue) {
		List<QuestTransition> matches = recoveryRoutes(definition).stream()
			.filter(candidate -> candidate.conditions().stream()
				.anyMatch(condition -> condition instanceof QuestCondition.QuestVariableIs variable
					&& variable.value() == fromValue))
			.toList();
		assertEquals(1, matches.size(), () -> "expected one recovery route from var0=" + fromValue);
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, int var0) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.put("var0", var0);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}
}
