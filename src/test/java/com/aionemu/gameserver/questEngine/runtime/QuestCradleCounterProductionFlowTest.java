package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证永恒摇篮双阵营狩猎任务的实时计数不会被 source 投影锁死，
 * 并在第 5 次或第 10 次有效击杀时立即进入领奖。
 * Verifies that faction-paired Cradle hunts keep live counters outside the source projection and enter reward on the
 * 5th or 10th valid kill.
 */
class QuestCradleCounterProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16828, 806282, Set.of(220470, 220471, 220472, 220594),
			220470, 5, true, true, true),
		new QuestContract(26828, 806287, Set.of(220470, 220471, 220472, 220473, 220594),
			220470, 5, false, false, false),
		/* P5-1：真端表 16829 的 5 个怪名解析出 5 个 id（与镜像 26829 同集）；旧 XML 的 8 id 并集
		   含另一任务的镜像目标，随退役退出。
		   Retail resolves 5 ids (same set as its mirror 26829); the legacy 8-id union retired. */
		new QuestContract(16829, 806282, Set.of(220474, 220475, 220476, 220477, 220479),
			220474, 10, true, true, true),
		new QuestContract(26829, 806287, Set.of(220474, 220475, 220476, 220477, 220479),
			220474, 10, true, false, false));

	@TestFactory
	Stream<DynamicTest> completesCradleHuntsOnTheConfiguredFinalKill() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		/* P5-1 网格 + P0-2 规范形：Cradle 行由真端击杀网格驱动（var0 = 计数 a0..a<required>，击杀边
		   无条件 PACKET_ONLY，满段 QUEST_SELECT 直翻领奖）；旧双变量行号形随退役退出。
		   Grid since P5-1 and canonical since P0-2: var0 counts kills a0..a<required>; kill edges
		   are unconditional and the saturated QUEST_SELECT flips reward directly. */
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		int required = contract.requiredKills();
		QuestNode zero = nodeByKillProjection(definition, QuestStatus.START, Map.of("var0", 0));
		QuestNode full = nodeByKillProjection(definition, QuestStatus.START, Map.of("var0", required));
		QuestNode reward = nodeByKillProjection(definition, QuestStatus.REWARD, Map.of("var0", required));
		int reportNpc = contract.reportNpcId();

		QuestEvent kill = new QuestEvent.KillNpc(contract.sampleTargetNpcId());
		QuestTransition continuing = transition(definition, zero.label(), nodeByKillProjection(definition,
			QuestStatus.START, Map.of("var0", 1)).label(), kill);
		assertEquals(List.of(), continuing.conditions());
		assertEquals(List.of(), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		// 满段交付协议（P0-2 规范形）：QUEST_SELECT 直翻领奖（LEVEL + 分档奖励窗，1009 中转删除）。
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition completion = transition(definition, full.label(), reward.label(), reportEvent);
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			completion.afterCommit());

		// 未满段：无 QUEST_SELECT/1009 报告通道，提前上交不可达。
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			zero.label().equals(candidate.sourceNode()) && candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "quest " + contract.questId() + " 未满段不得保留报告通道路由");
		assertNoPrematureTurnIn(compiled, definition, zero, reportNpc);

		// 击杀走查：1..required 全部留在 START，满段后报告才进领奖。
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= required; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, kill));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count), unpack(definition, snapshot));
		}
		QuestMutationPlan reportPlan = dispatch(compiled, snapshot, reportEvent);
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
		assertEquals(reward.projection().variables(),
			definition.progressLayout().unpack(reportPlan.nextPackedVariables()));

		assertRewardPages(definition, reward, reportNpc);
	}

	/** 未满段 1009 由满格条件门控：零段快照不可达上交。 / Early turn-in gated at the zero segment. */
	private static void assertNoPrematureTurnIn(CompiledQuestDefinition compiled, QuestDefinition definition,
			QuestNode zero, int reportNpc) {
		// 规范形下提前上交不可达：零段没有任何进领奖的对话路由（1009 亦无）。
		// In the canonical shape early turn-in is unreachable: the zero segment carries no dialog
		// route into reward (1009 included).
		QuestEvent earlyReport = new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertTrue(definition.transitions().stream().noneMatch(t ->
			zero.label().equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event().equals(earlyReport)),
			() -> "quest " + definition.id() + " early turn-in must stay unreachable");
	}

	private static List<QuestCondition> continuingConditions(QuestContract contract) {
		List<QuestCondition> conditions = new ArrayList<>();
		if (contract.guardsVar0()) {
			conditions.add(new QuestCondition.QuestVariableIs("var0", 0));
		}
		conditions.add(new QuestCondition.VariableBelow("var1", contract.requiredKills() - 1));
		return conditions;
	}

	private static List<QuestCondition> completionConditions(QuestContract contract) {
		List<QuestCondition> conditions = new ArrayList<>();
		if (contract.guardsVar0()) {
			conditions.add(new QuestCondition.QuestVariableIs("var0", 0));
		}
		conditions.add(contract.exactFinalCounter()
			? new QuestCondition.QuestVariableIs("var1", contract.requiredKills() - 1)
			: new QuestCondition.VariableAtLeast("var1", contract.requiredKills() - 1));
		return conditions;
	}

	private static List<QuestAction> completionActions(QuestContract contract) {
		if (contract.incrementOnFinalKill()) {
			return List.of(new QuestAction.IncrementVariable("var1", 1));
		}
		return List.of(
			new QuestAction.SetVariable("var1", contract.requiredKills()),
			new QuestAction.SetVariable("var0", 1));
	}

	private static void assertRewardPages(QuestDefinition definition, QuestNode reward, int reportNpcId) {
		/* 真端网格行在 REWARD 态无 QUEST_SELECT 重开路由（P0c-8c transXmlOnly 判例）；
		   1009/USE_OBJECT 预览再开领奖窗。 */
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			reward.label().equals(candidate.sourceNode()) && reward.label().equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			() -> "quest " + definition.id() + " must not keep the legacy reward-state re-open route");
		for (int dialogId : List.of(QuestDialogAction.SELECT_QUEST_REWARD.id(),
				QuestDialogAction.USE_OBJECT.id())) {
			QuestTransition preview = transition(definition, reward.label(), reward.label(),
				new QuestEvent.TalkToNpc(reportNpcId, dialogId));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status,
			Map<String, Integer> variables, QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static Map<String, Integer> unpack(QuestDefinition definition, QuestSnapshot snapshot) {
		return definition.progressLayout().unpack(snapshot.packedVariables());
	}


	/** (状态, 击杀投影) 寻址：忽略 var5 简报标志位。 / Kill-projection addressing, ignoring the flag. */
	private static QuestNode nodeByKillProjection(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> killVars) {
		return definition.nodes().stream()
			.filter(candidate -> candidate.projection().status() == status)
			.filter(candidate -> {
				Map<String, Integer> trimmed = new java.util.LinkedHashMap<>(candidate.projection().variables());
				trimmed.remove("var5");
				return trimmed.equals(killVars);
			})
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) {
		// 行已由真端表驱动（退役），改从生产视图取定义。
		// The rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}

	/**
	 * 保存四个任务的目标、阈值条件和末次计数动作差异。
	 * Holds target, threshold-condition, and final-counter action differences for the four quests.
	 */
	private record QuestContract(int questId, int reportNpcId, Set<Integer> targetNpcIds,
			int sampleTargetNpcId, int requiredKills, boolean guardsVar0,
			boolean exactFinalCounter, boolean incrementOnFinalKill) {
	}
}
