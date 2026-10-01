package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 26802（16802 的魔族孪生）的真端区域顺序 SECTION 链合同（定义来自生产视图）：
 * <ul>
 * <li>接取 = 进区域系统发放（{@code SystemGrant} + {@code StartEligible}）；旧 XML 的 LevelUp/
 * ZoneMissionEnd 接取路由、SELECT_NONE 提供页与 finished:26801 门控随退役入 git 历史；</li>
 * <li>进度 = 2 段顺序链（30 名 Leibo 图书管理员 → 2 只 BI Leibo 首领），击杀边只推进首个未满段，
 * 乱序不计；</li>
 * <li>报告：未满链节点 QUEST_SELECT 显示客户端完成页、1009 带双段满门禁；满节点 a30b2 无门禁进领奖；
 * 完成流按真端元数据奖励收尾 CompleteQuest。</li>
 * </ul>
 * Verifies the retail area-driven sequential SECTION chain of quest 26802 (the Asmodian twin of
 * 16802, production-view definitions): area-entry system grant without legacy accept routes, the
 * two-stage kill chain advancing only the first unfinished stage, gated recovery on unfinished nodes,
 * and the metadata-driven completion flow.
 */
class Quest26802ClientDialogAlignmentTest {
	private static final int QUEST_ID = 26802;
	private static final int DIALOG_NPC_ID = 806149;
	private static final int STAGE_ONE_KILLS = 30;
	private static final int STAGE_TWO_KILLS = 2;
	/** 客户端段 1 清单（Leibo 图书管理员 8 变体）。 / Client stage-1 list (8 Leibo librarians). */
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	/** 客户端段 2 清单（BI Leibo 首领 6 变体）。 / Client stage-2 list (6 BI Leibo sub-bosses). */
	private static final Set<Integer> SUB_BOSSES = Set.of(
		857450, 857452, 857454, 857456, 857458, 857459);

	@Test
	void areaGrantStartsTheSequentialChainWithoutNpcRoutes() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		for (int first = 0; first <= STAGE_ONE_KILLS; first++) {
			assertNode(definition, "a" + first + "b0", QuestStatus.START,
				Map.of("var0", first, "var1", 0));
		}
		assertNode(definition, "a" + STAGE_ONE_KILLS + "b1", QuestStatus.START,
			Map.of("var0", STAGE_ONE_KILLS, "var1", 1));
		assertNode(definition, "a" + STAGE_ONE_KILLS + "b" + STAGE_TWO_KILLS, QuestStatus.START,
			Map.of("var0", STAGE_ONE_KILLS, "var1", STAGE_TWO_KILLS));
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var0", STAGE_ONE_KILLS, "var1", STAGE_TWO_KILLS));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

		QuestTransition grant = transition(definition, "unaccepted", "a0b0", new QuestEvent.SystemGrant());
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(), grant.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());
		// 进区域发放行不得有任何接取路由：旧 XML 的 LevelUp/ZoneMissionEnd/QUEST_ACCEPT_SIMPLE
		// 与 SELECT_NONE 提供页随真端链一并移除。
		// Area-granted rows keep no accept routes: the legacy LevelUp/ZoneMissionEnd routes, the
		// simple accept and the SELECT_NONE offer page all retired with the XML.
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				"unaccepted".equals(candidate.sourceNode())
				&& (candidate.event() instanceof QuestEvent.TalkToNpc
					|| candidate.event() instanceof QuestEvent.LevelUp
					|| candidate.event() instanceof QuestEvent.ZoneMissionEnd)),
			"area-granted rows must not keep legacy accept routes");

		// 未满链节点：无 QUEST_SELECT/1009 报告通道（P0-2 顺序链规范形，页链不再由服务端驱动，
		// 提前上交不可达；FINISH_DIALOG 关窗出口保留）。
		// Unfinished chain nodes: no QUEST_SELECT/1009 report channel (canonical sequential shape
		// since P0-2; early turn-in is unreachable; the FINISH_DIALOG close exit stays).
		for (int first = 0; first < STAGE_ONE_KILLS; first++) {
			final String node = "a" + first + "b0";
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
				node.equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == DIALOG_NPC_ID && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> node + " 不得保留报告通道路由");
		}

		// 满节点：QUEST_SELECT 无门禁直翻领奖并按档位查表下发奖励窗（1009 中转删除）。
		// Full node: the QUEST_SELECT flips reward ungated with the tiered window (no 1009 hop).
		String fullLabel = "a" + STAGE_ONE_KILLS + "b" + STAGE_TWO_KILLS;
		QuestTransition finish = talk(definition, fullLabel, "reward",
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), finish.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			finish.afterCommit());

		// 完成流：确认段全覆盖、真端元数据奖励 + CompleteQuest 收尾。
		// Completion: the full confirm range granting the retail metadata rewards ends in CompleteQuest.
		assertCompletionFlow(definition, DIALOG_NPC_ID);
	}

	@Test
	void killEdgesAdvanceOnlyTheFirstUnfinishedStage() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();

		// 链上击杀边：段 1 状态只挂图书管理员，段 2 状态只挂 BI 首领；每条边推进到下一链节点。
		// Chain kill edges: stage-1 states carry only librarians, the stage-2 state only sub-bosses;
		// every edge advances to the next chain node.
		Map<String, Integer> zero = Map.of("var0", 0, "var1", 0);
		List<Set<Integer>> perStateTargets = new ArrayList<>();
		for (int first = 0; first < STAGE_ONE_KILLS; first++) {
			perStateTargets.add(assertChainEdges(definition, "a" + first + "b0", "a" + (first + 1) + "b0"));
		}
		perStateTargets.add(assertChainEdges(definition, "a" + STAGE_ONE_KILLS + "b0",
			"a" + STAGE_ONE_KILLS + "b1"));
		perStateTargets.add(assertChainEdges(definition, "a" + STAGE_ONE_KILLS + "b1",
			"a" + STAGE_ONE_KILLS + "b" + STAGE_TWO_KILLS));
		Set<Integer> stageOneTargets = new TreeSet<>();
		perStateTargets.subList(0, STAGE_ONE_KILLS).forEach(stageOneTargets::addAll);
		Set<Integer> stageTwoTargets = perStateTargets.getLast();
		assertTrue(stageOneTargets.containsAll(LIBRARIANS),
			() -> "stage 1 kill set must cover the client librarians, got " + stageOneTargets);
		assertTrue(stageTwoTargets.containsAll(SUB_BOSSES),
			() -> "stage 2 kill set must cover the client sub-bosses, got " + stageTwoTargets);
		assertTrue(java.util.Collections.disjoint(stageOneTargets, stageTwoTargets),
			"stage target sets must stay disjoint");

		// 乱序不计：段 1 未满时段 2 样本不得产生任何击杀计划。
		// Out-of-order kills never count: a stage-2 sample produces no kill plan while stage 1 runs.
		assertNoMatch(compiled, snapshot(compiled, zero), new QuestEvent.KillNpc(857450));

		// 顺序推进模拟：30 名图书管理员只推进段 1；随后 2 只首领推进段 2 且末杀仍停在 START。
		// Sequential walk: 30 librarians fill stage 1 only; the two sub-boss kills then fill stage 2,
		// and even the final kill stays START — only the report enters reward.
		QuestSnapshot current = snapshot(compiled, zero);
		for (int kill = 1; kill <= STAGE_ONE_KILLS; kill++) {
			current = nextSnapshot(current, dispatch(compiled, current, new QuestEvent.KillNpc(220306)));
			assertEquals(Map.of("var0", kill, "var1", 0),
				definition.progressLayout().unpack(current.packedVariables()),
				"stage-1 kill " + kill + " must keep stage 2 untouched");
		}
		assertNoMatch(compiled, current, new QuestEvent.KillNpc(220306));
		current = nextSnapshot(current, dispatch(compiled, current, new QuestEvent.KillNpc(857450)));
		assertEquals(Map.of("var0", STAGE_ONE_KILLS, "var1", 1),
			definition.progressLayout().unpack(current.packedVariables()));
		assertEquals(QuestStatus.START, current.status());
		current = nextSnapshot(current, dispatch(compiled, current, new QuestEvent.KillNpc(857459)));
		assertEquals(QuestStatus.START, current.status());
		assertEquals(Map.of("var0", STAGE_ONE_KILLS, "var1", STAGE_TWO_KILLS),
			definition.progressLayout().unpack(current.packedVariables()));
		assertNoMatch(compiled, current, new QuestEvent.KillNpc(857450));

		// 满链 QUEST_SELECT 交付进领奖（P0-2 顺序链规范形）。
		// The full chain's QUEST_SELECT delivery enters reward (canonical sequential shape).
		QuestMutationPlan report = dispatch(compiled, current,
			new QuestEvent.TalkToNpc(DIALOG_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", STAGE_ONE_KILLS, "var1", STAGE_TWO_KILLS),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	/** 链节点击杀边合同：目标集合、推进目标与 PACKET_ONLY。 / Chain-node kill-edge contract. */
	private static Set<Integer> assertChainEdges(QuestDefinition definition, String source,
			String target) {
		List<QuestTransition> edges = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
			.toList();
		assertFalse(edges.isEmpty(), () -> source + " must carry the current stage's kill edges");
		Set<Integer> targets = new TreeSet<>();
		for (QuestTransition edge : edges) {
			assertEquals(target, edge.targetNode(), edge::toString);
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				edge.afterCommit(), edge::toString);
			assertTrue(edge.event() instanceof QuestEvent.KillNpc killNpc && targets.add(killNpc.npcId()),
				edge::toString);
		}
		return targets;
	}

	/** 完成流合同：确认段 8..23 全覆盖、真端元数据奖励 + CompleteQuest 收尾。 / Completion contract. */
	private static void assertCompletionFlow(QuestDefinition definition, int rewardNpc) {
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.toList();
		assertFalse(completions.isEmpty(), "the reward state must complete via the confirm range");
		Set<Integer> actionIds = new TreeSet<>();
		for (QuestTransition completion : completions) {
			assertTrue(completion.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == rewardNpc
					&& talk.dialogId() >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& talk.dialogId() <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(),
				() -> "completion routes must hang on the confirm range " + completion);
			assertTrue(completion.actions().stream().anyMatch(action ->
					action instanceof QuestAction.CompleteQuest),
				() -> "completion routes must end in CompleteQuest " + completion);
			assertTrue(completion.actions().stream().anyMatch(action ->
					action instanceof QuestAction.GrantReward),
				() -> "completion routes must grant the retail metadata rewards " + completion);
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit(), completion::toString);
			if (completion.event() instanceof QuestEvent.TalkToNpc talk) {
				actionIds.add(talk.dialogId());
			}
		}
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			final int dialogId = id;
			assertTrue(actionIds.contains(dialogId), () -> "confirm range misses dialogId " + dialogId);
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

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled,
			Map<String, Integer> variables) {
		return new QuestSnapshot(7, compiled.id(), QuestStatus.START,
			compiled.definition().progressLayout().pack(variables), Map.of());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
			.filter(candidate -> Objects.equals(candidate.targetNode(), target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition() {
		return ProductionQuestDefinitions.definition(QUEST_ID);
	}
}
