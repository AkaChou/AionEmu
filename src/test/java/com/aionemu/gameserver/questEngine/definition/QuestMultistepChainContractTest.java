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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定「客户端任务书逐行对话链」批量修复合同（13 个任务）。
 * Locks the batch repair contract for client journal row-by-row talk chains (13 quests).
 *
 * <p>这一族的原始缺陷是同一形状：客户端 quest_summary 逐行列出 n 个步骤（末行领奖），
 * 但服务端把整条链压成一个 {@code started(var0=0)} 状态，所有步骤 NPC 共用同一个
 * {@code HACTION_SETPRO1} 并直接跳 reward。玩家跟第 1 个 NPC 说完话就能领奖，第 2..n 行永远不可达；
 * wiki 侧的表现是每一步的 GM 命令都等于 {@code //quest set <id> START 0}。
 * The defect family shares one shape: the client journal lists n rows (last one rewards) while the
 * server compressed the chain into a single {@code started(var0=0)} state whose NPCs all shared one
 * {@code HACTION_SETPRO1} and jumped straight to reward, so row 2..n were unreachable and every wiki
 * GM command collapsed to {@code //quest set <id> START 0}.</p>
 *
 * <p>修复后的合同与已提交的 1192 一致：每行一个 START 状态（var0 = 行号 0..n-1），末行另有同 var0 的
 * REWARD 状态；第 i 行由该行客户端 NPC 的 {@code SETPROi} 推进到下一行状态，领奖行由自己的客户端
 * {@code SELECT_QUEST_REWARD} 进入 REWARD，入口页取自该行客户端链；只有领奖行 NPC 能完成任务。
 * The repaired contract matches the committed 1192: one START state per row (var0 = row index 0..n-1) plus a
 * REWARD state on the last var0; row i advances through that row's client NPC with {@code SETPROi}, the reward
 * row enters REWARD through its own client {@code SELECT_QUEST_REWARD}, entry pages come from the client chain,
 * and only the reward row NPC may complete the quest.</p>
 */
class QuestMultistepChainContractTest {
	private record Step(int npcId, QuestDialogPage page) {
	}

	private record Chain(int questId, int rows, List<Step> steps, int rewardNpc, QuestDialogPage rewardPage) {
	}

	/**
	 * 逐任务的三方核对结果（客户端 quest_summary 行 → 客户端对话链 → retail 步骤），
	 * 由 .agents/summary/quest-multistep-contract-batch 的 dump_chain_evidence.py 导出。
	 * Per-quest three-way evidence (client quest_summary rows, client dialog chains, retail steps).
	 */
	private static final List<Chain> CHAINS = List.of(
		new Chain(1183, 3, List.of(
			new Step(730013, QuestDialogPage.SELECT2),
			new Step(730014, QuestDialogPage.SELECT3)), 730012, QuestDialogPage.SELECT5),
		new Chain(1319, 9, List.of(
			new Step(203923, QuestDialogPage.SELECT2),
			new Step(203910, QuestDialogPage.SELECT3),
			new Step(203906, QuestDialogPage.SELECT4),
			new Step(203915, QuestDialogPage.SELECT5),
			new Step(203907, QuestDialogPage.SELECT6),
			new Step(798050, QuestDialogPage.SELECT7),
			new Step(798049, QuestDialogPage.SELECT8),
			new Step(205240, QuestDialogPage.SELECT9)), 203908, QuestDialogPage.SELECT10),
		new Chain(1483, 3, List.of(
			new Step(203940, QuestDialogPage.SELECT2),
			new Step(203944, QuestDialogPage.SELECT3)), 798127, QuestDialogPage.SELECT5),
		new Chain(1514, 3, List.of(
			new Step(204582, QuestDialogPage.SELECT2),
			new Step(204505, QuestDialogPage.SELECT3)), 203831, QuestDialogPage.SELECT5),
		new Chain(1721, 3, List.of(
			new Step(278503, QuestDialogPage.SELECT2),
			new Step(278502, QuestDialogPage.SELECT3)), 278518, QuestDialogPage.SELECT5),
		new Chain(1724, 3, List.of(
			new Step(278591, QuestDialogPage.SELECT2),
			new Step(278599, QuestDialogPage.SELECT3)), 278594, QuestDialogPage.SELECT5),
		new Chain(2449, 2, List.of(
			new Step(798115, QuestDialogPage.SELECT1)), 798080, QuestDialogPage.DEFAULT_SUCCESS),
		new Chain(2646, 4, List.of(
			new Step(204777, QuestDialogPage.SELECT2),
			new Step(204700, QuestDialogPage.SELECT3),
			new Step(204702, QuestDialogPage.SELECT4)), 204817, QuestDialogPage.SELECT5),
		new Chain(2692, 4, List.of(
			new Step(204108, QuestDialogPage.SELECT2),
			new Step(279027, QuestDialogPage.SELECT3),
			new Step(279029, QuestDialogPage.SELECT4)), 212164, QuestDialogPage.SELECT5),
		new Chain(2767, 3, List.of(
			new Step(279026, QuestDialogPage.SELECT2),
			new Step(279061, QuestDialogPage.SELECT3)), 279004, QuestDialogPage.SELECT5),
		new Chain(3966, 4, List.of(
			new Step(203994, QuestDialogPage.SELECT2),
			new Step(204030, QuestDialogPage.SELECT3),
			new Step(204568, QuestDialogPage.SELECT4)), 798391, QuestDialogPage.SELECT5),
		new Chain(3968, 4, List.of(
			new Step(798176, QuestDialogPage.SELECT2),
			new Step(204528, QuestDialogPage.SELECT3),
			new Step(203927, QuestDialogPage.SELECT4)), 798390, QuestDialogPage.SELECT5),
		new Chain(4501, 3, List.of(
			new Step(204340, QuestDialogPage.SELECT2),
			new Step(204348, QuestDialogPage.SELECT3)), 204728, QuestDialogPage.SELECT5));

	@Test
	void everyJournalRowOwnsItsOwnProgressState() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());

			// 每一行都要有自己的 START 状态（var0=行号）；旧定义只有 var0=0，第 2 行起没有任何状态，
			// 于是 wiki 里每个步骤的 GM 命令都退化成 //quest set <id> START 0。
			List<Integer> startRows = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START)
				.map(node -> node.projection().variables().get("var0"))
				.sorted()
				.toList();
			assertEquals(IntStream.range(0, chain.rows()).boxed().toList(), startRows,
				"quest " + chain.questId() + " 的 START 状态必须逐行覆盖 var0=0.." + (chain.rows() - 1));

			// 领奖行额外拥有同 var0 的 REWARD 状态。
			assertTrue(definition.nodes().stream()
					.anyMatch(node -> node.projection().status() == QuestStatus.REWARD
						&& node.projection().variables().get("var0") == chain.rows() - 1),
				"quest " + chain.questId() + " 的领奖行必须是 var0=" + (chain.rows() - 1));
		}
	}

	@Test
	void everyStepAdvancesItsOwnStateThroughItsOwnSetproAction() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			for (int index = 0; index < chain.steps().size(); index++) {
				Step step = chain.steps().get(index);
				String source = startLabel(definition, index);
				String target = startLabel(definition, index + 1);

				// 入口页：该行客户端 NPC 的 QUEST_SELECT 必须下发该行客户端链的首页。
				assertTrue(talk(definition, source, source, step.npcId(),
						QuestDialogAction.QUEST_SELECT.id()).afterCommit()
						.contains(new AfterCommitAction.ShowQuestDialog(step.page().id())),
					"quest " + chain.questId() + " 步骤 " + (index + 1) + " 的入口页应为 " + step.page());

				// 推进：该行客户端 NPC 的 SETPRO{i} 从第 i 个状态进入第 i+1 个状态。
				QuestEvent advance = new QuestEvent.TalkToNpc(step.npcId(),
					QuestDialogAction.valueOf("SETPRO" + (index + 1)).id());
				assertTrue(definition.transitions().stream().anyMatch(transition ->
						source.equals(transition.sourceNode()) && target.equals(transition.targetNode())
							&& advance.equals(transition.event())),
					"quest " + chain.questId() + " 步骤 " + (index + 1) + " 缺少 " + source + " -> " + target
						+ " 的 SETPRO" + (index + 1) + " 路由");
			}
		}
	}

	@Test
	void setproActionsStayOnePerStepInsteadOfAllSharingSetproOne() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			List<String> actual = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> (QuestEvent.TalkToNpc) transition.event())
				.filter(talk -> talk.dialogId() != null)
				.map(talk -> QuestDialogAction.fromId(talk.dialogId()).name())
				.filter(name -> name.startsWith("SETPRO"))
				.sorted()
				.toList();
			List<String> expected = IntStream.rangeClosed(1, chain.steps().size())
				.mapToObj(index -> "SETPRO" + index)
				.sorted()
				.toList();
			assertEquals(expected, actual,
				"quest " + chain.questId() + " 的推进动作必须一步一个，不能所有步骤共用 SETPRO1");
		}
	}

	@Test
	void onlyTheRewardRowNpcOwnsRewardRowRoutesAndCompletion() throws Exception {
		for (Chain chain : CHAINS) {
			QuestDefinition definition = definition(chain.questId());
			String rewardRow = startLabel(definition, chain.rows() - 1);
			String reward = rewardLabel(definition, chain.rows() - 1);

			// 领奖行的所有对话路由（领奖页、提交页）只能属于领奖行 NPC。
			List<Integer> npcs = definition.transitions().stream()
				.filter(transition -> rewardRow.equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.distinct()
				.sorted()
				.toList();
			assertEquals(List.of(chain.rewardNpc()), npcs,
				"quest " + chain.questId() + " 的领奖行只能由 " + chain.rewardNpc() + " 承担");

			// 领奖行的客户端提交动作进入 REWARD（不是由上一步的 SETPRO 直接跳进领奖态）。
			assertTrue(definition.transitions().stream().anyMatch(transition ->
					rewardRow.equals(transition.sourceNode()) && reward.equals(transition.targetNode())
						&& new QuestEvent.TalkToNpc(chain.rewardNpc(),
							QuestDialogAction.SELECT_QUEST_REWARD.id()).equals(transition.event())),
				"quest " + chain.questId() + " 缺少领奖行进入 REWARD 的 SELECT_QUEST_REWARD 路由");

			String complete = completeLabel(definition);
			assertEquals(List.of(chain.rewardNpc()), definition.transitions().stream()
					.filter(transition -> reward.equals(transition.sourceNode())
						&& complete.equals(transition.targetNode()))
					.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
					.distinct()
					.sorted()
					.toList(),
				"quest " + chain.questId() + " 只有领奖行 NPC 能完成任务");
		}
	}

	@Test
	void theChainWalksRowByRowAtRuntime() throws Exception {
		for (Chain chain : CHAINS) {
			CompiledQuestDefinition compiled = compiled(chain.questId());
			QuestDefinition definition = compiled.definition();
			Map<String, Integer> variables = new LinkedHashMap<>(definition.progressLayout().unpack(0));
			QuestStatus status = QuestStatus.START;

			for (int index = 0; index < chain.steps().size(); index++) {
				Step step = chain.steps().get(index);
				QuestTransition advance = talk(definition, startLabel(definition, index),
					startLabel(definition, index + 1), step.npcId(),
					QuestDialogAction.valueOf("SETPRO" + (index + 1)).id());

				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, status, variables), advance.event(), advance).orElseThrow();
				status = plan.nextStatus();
				variables = definition.progressLayout().unpack(plan.nextPackedVariables());

				assertEquals(index + 1, variables.get("var0"),
					"quest " + chain.questId() + " 第 " + (index + 1) + " 步后应停在 var0=" + (index + 1));
				assertEquals(QuestStatus.START, status,
					"quest " + chain.questId() + " 第 " + (index + 1) + " 步后仍是进行中（领奖行尚未提交）");
			}

			// 领奖行自己的客户端提交动作才把第 rows-1 行推进到 REWARD。
			QuestTransition turnIn = talk(definition, startLabel(definition, chain.rows() - 1),
				rewardLabel(definition, chain.rows() - 1), chain.rewardNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD.id());
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, status, variables), turnIn.event(), turnIn).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				"quest " + chain.questId() + " 领奖行提交后应进入 REWARD");
			assertEquals(chain.rows() - 1,
				definition.progressLayout().unpack(plan.nextPackedVariables()).get("var0"),
				"quest " + chain.questId() + " 领奖行的 var0 必须停在末行行号");
		}
	}

	private static String startLabel(QuestDefinition definition, int var0) {
		return labelOf(definition, QuestStatus.START, var0);
	}

	private static String rewardLabel(QuestDefinition definition, int var0) {
		return labelOf(definition, QuestStatus.REWARD, var0);
	}

	private static String completeLabel(QuestDefinition definition) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.COMPLETE)
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), "必须声明唯一的 COMPLETE 状态");
		return labels.getFirst();
	}

	private static String labelOf(QuestDefinition definition, QuestStatus status, int var0) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().get("var0") == var0)
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), "quest " + definition.id() + " 的 " + status + "(var0=" + var0 + ") 状态必须唯一");
		return labels.getFirst();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& new QuestEvent.TalkToNpc(npcId, action).equals(candidate.event()))
			.findFirst().orElseThrow(() -> new AssertionError(
				"missing route " + source + " -> " + target + " npc=" + npcId + " action=" + action));
	}

	private static QuestDefinition definition(int questId) throws Exception {
		return compiled(questId).definition();
	}

	private static CompiledQuestDefinition compiled(int questId) throws Exception {
		try (InputStream input = QuestMultistepChainContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(variables),
			workItems(definition), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	/**
	 * 领奖行的交付条件（如 3966/3968 的 has-item）要求背包持有任务物品，
	 * 否则条件路由不会命中，测试无法走到 REWARD。
	 * Turn-in conditions (e.g. the has-item gate on 3966/3968) require the work item in the inventory,
	 * otherwise the conditional route never resolves and the walk cannot reach REWARD.
	 */
	private static Map<Integer, Integer> workItems(CompiledQuestDefinition definition) {
		// <items>（itemRequirements）与 <work-items> 都可能声明交付物品，测试快照两者都要带上。
		// Both <items> (itemRequirements) and <work-items> may declare the turn-in item.
		Map<Integer, Integer> inventory = new LinkedHashMap<>();
		for (QuestItemRequirement requirement : definition.definition().metadata().itemRequirements()) {
			inventory.put(requirement.itemId(), requirement.count());
		}
		for (QuestItemRequirement requirement : definition.definition().metadata().questWorkItems()) {
			inventory.put(requirement.itemId(), requirement.count());
		}
		return Map.copyOf(inventory);
	}
}
