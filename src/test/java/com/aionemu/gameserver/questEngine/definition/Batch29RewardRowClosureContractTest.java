package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 29：剩余 MISSING_LAST_ROW 里“legacy 落盘 step 与 XML reward 投影不一致”的 6 个真缺陷。
 * <p>
 * 判据（QE-054，与批次 27/28 同口径）：旧引擎 {@code QuestHandler.changeQuestStep(env, step, nextStep, true)}
 * 的 reward 分支只 {@code setStatus(REWARD)}、**不写 nextStep**，所以领奖态真正落盘的 step 恒等于调用时的
 * {@code step}（也就是进入领奖时已有的 var0）；只有 {@code qs.setQuestVar(N)} /
 * {@code changeQuestStep(..., false)} / {@code setQuestVarById(0, N)} 之后的 {@code setStatus(REWARD)}
 * 才把 var0 写成 N。逐任务的取证见
 * {@code .agents/summary/quest-10527-reward-row/batch29-triage.tsv} 与 {@code batch29-evidence.tsv}：
 * <ul>
 *   <li>1876/2876（Taranis / Votan Emergency Orders）：{@code changeQuestStep(env, 1, 2, false)} 写 2
 *       之后 {@code setStatus(REWARD)} → 落盘 2；旧 XML 投影写成 1，领奖时任务书停在行 1；</li>
 *   <li>14123（The Shadow Of Vengeance）：{@code defaultOnKillEvent(env, 206360, 0, 1)} 写 1，
 *       {@code SELECT_REWARD} 只 setStatus → 落盘 1；旧 XML 的 4 条 report -> reward 交接又显式写
 *       {@code var0=0}，把领奖态打回行 0；</li>
 *   <li>2600（Humongous Malek）：legacy 只在 var0 == 1 时响应 {@code SELECT_REWARD} → 落盘 1；
 *       旧 XML 投影写成 0；</li>
 *   <li>11010（Angel To The Wounded）：{@code defaultCloseDialog(env, 2, 3)} 写 3，随后 var0 == 3
 *       的 {@code SELECT_REWARD} 只 setStatus → 落盘 3；旧 XML 投影写成 0；</li>
 *   <li>1466（Respect For Deltras）：两条进入领奖的路径分别落盘 0（道具）与 2（203903 显式
 *       {@code setQuestVar(2)}），而客户端 quest_summary 只有 2 行（0..1），越界的 2 会让任务书空白；
 *       本批统一落盘 1（= 末行“向 Valerius 报告”）。</li>
 * </ul>
 * 门禁同时锁定：每条进入领奖的路线都不得写过期的行号、旧存档进入世界时自愈、正规态不被重放。
 * <p>
 * Locks batch 29: the six quests whose XML reward projection disagreed with the step persisted by the
 * legacy handler on entering REWARD (1876/2876/14123/2600/11010/1466), including the stale-save recovery
 * edges and the rule that no reward-entry route may write an outdated journal row.
 */
class Batch29RewardRowClosureContractTest {

	/** 任务 id / 领奖态应落盘的行 / 旧存档可能停留的行（自愈边的匹配值）。 */
	private record Contract(int questId, int rewardRow, List<Integer> staleRows) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(1876, 2, List.of(1)),
		new Contract(2876, 2, List.of(1)),
		new Contract(14123, 1, List.of(0)),
		new Contract(2600, 1, List.of(0)),
		new Contract(11010, 3, List.of(0)),
		new Contract(1466, 1, List.of(0, 2)));

	@Test
	void rewardProjectionMatchesTheLegacyPersistedRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", contract.rewardRow())),
				node(definition, "reward").projection(),
				() -> "quest " + contract.questId() + " reward projection");
		}
	}

	@Test
	void everyRewardEntryLandsOnThePersistedRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();
			List<QuestTransition> entries = definition.transitions().stream()
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.sourceNode() != null)
				.toList();
			assertTrue(!entries.isEmpty(), () -> "quest " + contract.questId() + " reward entries");
			for (QuestTransition route : entries) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " reward entry " + route.sourceNode() + " writes a stale journal row");
					}
				}
				// 用进入前的行规划这条路线：落点必须是领奖态 + 落盘行。
				for (int fromRow : candidateRows(definition)) {
					QuestSnapshot snapshot = snapshot(compiled, QuestStatus.START, Map.of("var0", fromRow));
					QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot, route)
						.orElse(null);
					if (plan == null) {
						continue;
					}
					assertEquals(QuestStatus.REWARD, plan.nextStatus(),
						() -> "quest " + contract.questId() + " plan status");
					assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
						() -> "quest " + contract.questId() + " reward row after "
							+ route.sourceNode() + " (from var0=" + fromRow + ")");
				}
			}
		}
	}

	@Test
	void staleRewardRowIsRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> recoveryRoutes = recoveryRoutes(compiled.definition());
			assertEquals(contract.staleRows().size(), recoveryRoutes.size(),
				() -> "quest " + contract.questId() + " recovery routes");
			for (int staleRow : contract.staleRows()) {
				QuestTransition recovery = recoveryRoutes.stream()
					.filter(route -> route.conditions().contains(
						new QuestCondition.QuestVariableIs("var0", staleRow)))
					.findFirst().orElseThrow(() -> new AssertionError("quest " + contract.questId()
						+ " has no recovery route for var0=" + staleRow));
				assertEquals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", staleRow)), recovery.conditions(),
					() -> "quest " + contract.questId() + " recovery conditions for var0=" + staleRow);
				assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())),
					recovery.actions(),
					() -> "quest " + contract.questId() + " recovery actions for var0=" + staleRow);
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
					() -> "quest " + contract.questId() + " recovery after-commit");
				assertNull(recovery.priority());

				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, QuestStatus.REWARD, Map.of("var0", staleRow)),
					recovery.event(), recovery).orElseThrow();
				assertEquals(QuestStatus.REWARD, plan.nextStatus());
				assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
					() -> "quest " + contract.questId() + " repaired journal row");
			}
		}
	}

	@Test
	void liveRewardRowIsNotReplayed() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			assertTrue(plans(compiled, QuestStatus.REWARD, Map.of("var0", contract.rewardRow()),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId()
					+ " must not replay the recovery edge for the live reward row");
		}
	}

	/** 进入前的行候选：进度字段声明范围内的所有值（含越界旧值，用于规划失败的容忍）。 */
	private static List<Integer> candidateRows(QuestDefinition definition) {
		BitField field = definition.progressLayout().field("var0");
		return java.util.stream.IntStream.rangeClosed(field.minValue(), field.maxValue()).boxed().toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static List<QuestTransition> recoveryRoutes(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, QuestEvent event) {
		QuestSnapshot snapshot = snapshot(compiled, status, variables);
		return compiled.definition().transitions().stream()
			.flatMap(route -> QuestMutationPlanner.plan(compiled, snapshot, event, route).stream())
			.toList();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(packedVariables), Map.of());
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch29RewardRowClosureContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
