package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 51：3938/4942（神圣圣殿骑士晋级双子）的任务书末两行状态与领奖投影。
 * <p>
 * 判据：客户端 {@code QUEST_Q3938.html}/{@code QUEST_Q4942.html} 的 {@code quest_summary} 各 11 行
 * （可见槽位 0/3/6/.../30）——行 0 = 接取 NPC 处选择制造技术、行 1..6 = 六种工艺名人分支、
 * 行 7 = 制造名人处交圣物 186000077、行 8 = 大神官处举行仪式（收仪式道具
 * 186000081/186000085）、行 9 = 仪式完成、行 10 = 回起始 NPC 领奖。迁移前 handler
 * {@code _3938Well_Rounded} 的阶梯是 0 → 1..6 → 7 → 8（大神官处 SET_REWARD）→ REWARD，
 * 末两行必须有独立状态；迁移后 START 阶梯只到 s8、reward 投影停在 8/0，行 9/10 永远不亮。
 * <p>
 * Locks batch 51: both halves of the Holy Templar advancement pair keep one START state per client
 * journal row (step 0..10), project REWARD at the last row (step 10), consume the ritual item on the
 * hand-over, and heal the old REWARD/var0&lt;10 saves to the last row.
 */
class HolyTemplarFinalRowPairContractTest {

	private static final int FINAL_STEP = 10;
	private static final QuestSpec ELYOS = new QuestSpec(3938, 203752, 186000081, 203701, 8);
	private static final QuestSpec ASMODIANS = new QuestSpec(4942, 204075, 186000085, 204053, 0);

	@Test
	void everyClientJournalRowOwnsItsStepState() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			Map<Integer, QuestStatus> steps = stepStates(definition);
			for (int step = 0; step <= FINAL_STEP; step++) {
				final int current = step;
				// step 9 = 大神官处仪式完成（START），step 10 = 回起始 NPC 领奖行（REWARD）。
				QuestStatus expected = current == FINAL_STEP ? QuestStatus.REWARD : QuestStatus.START;
				assertEquals(expected, steps.get(current),
					() -> "quest " + spec.questId() + " step " + current + " must own a journal state");
			}
			assertEquals(QuestStatus.REWARD, node(definition, "s10").projection().status(),
				() -> "quest " + spec.questId() + " claim row is the REWARD state");
			assertEquals(FINAL_STEP, node(definition, "s10").projection().variables().get("var0"),
				() -> "quest " + spec.questId() + " last client journal row is step 10");
			assertEquals(Map.of("var0", FINAL_STEP),
				definition.progressLayout().unpack(definition.progressLayout().pack(Map.of("var0", FINAL_STEP))),
				() -> "quest " + spec.questId() + " last row step fits its SECTION_0 bit field");
			assertTrue(definition.nodes().stream().noneMatch(candidate -> "reward".equals(candidate.label())),
				() -> "quest " + spec.questId() + " no longer keeps the collapsed reward node");
		}
	}

	@Test
	void theSixCraftRowsAdvanceSectionZeroWithoutSkipping() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			// 六条工艺分支：SETPRO1..6 显式写 var0=1..6，或由目标节点 s1..s6 的投影写入。
			// 两种写法在 planner 里等价（QE-002），门禁只要求每条分支都落到自己的行。
			for (int step = 0; step < 6; step++) {
				final int current = step;
				List<QuestTransition> routes = routes(definition, "started", "s" + (step + 1));
				assertEquals(1, routes.size(),
					() -> "quest " + spec.questId() + " craft branch " + current + " is a single route");
				int targetStep = node(definition, "s" + (step + 1)).projection().variables().get("var0");
				assertEquals(current + 1, targetStep,
					() -> "quest " + spec.questId() + " craft branch " + current + " targets its own row");
			}
			assertTrue(routes(definition, "started", "s7").isEmpty(),
				() -> "quest " + spec.questId() + " must not skip the artisan hand-over row");
		}
	}

	@Test
	void ritualRowConsumesTheClientItemAndLandsOnTheLastRow() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			List<QuestTransition> handOvers = routes(definition, "s8", "s9").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(spec.ritualNpc(),
					QuestDialogAction.SET_SUCCEED.id())))
				.toList();
			assertEquals(1, handOvers.size(),
				() -> "quest " + spec.questId() + " ritual SET_SUCCEED route is a single route");
			QuestTransition handOver = handOvers.getFirst();
			assertEquals(0, handOver.priority(),
				() -> "quest " + spec.questId() + " gated ritual route wins over the missing-item page");
			assertTrue(handOver.conditions().contains(new QuestCondition.QuestVariableIs("var0", 8)),
				() -> "quest " + spec.questId() + " ritual route is gated on its own row");
			assertTrue(handOver.conditions().contains(new QuestCondition.HasItem(spec.ritualItem(), 1)),
				() -> "quest " + spec.questId() + " ritual route requires the ceremony item");
			assertTrue(handOver.actions().contains(new QuestAction.RemoveItem(spec.ritualItem(), 1)),
				() -> "quest " + spec.questId() + " consumes the ceremony item");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.CloseDialog()),
				handOver.afterCommit(),
				() -> "quest " + spec.questId() + " refreshes the journal after the ritual");
		}
	}

	@Test
	void startingNpcClaimRowMovesToStepTenAndShowsTheRewardWindow() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			List<QuestTransition> claims = routes(definition, "s9", "s10").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(spec.handoverNpc(),
					QuestDialogAction.SELECT_QUEST_REWARD.id())))
				.toList();
			assertEquals(1, claims.size(),
				() -> "quest " + spec.questId() + " claim row is a single SELECT_QUEST_REWARD route");
			QuestTransition claim = claims.getFirst();
			assertEquals(0, claim.priority(),
				() -> "quest " + spec.questId() + " claim route wins over the success-page fallback");
			assertTrue(claim.conditions().contains(new QuestCondition.QuestVariableIs("var0", 9)),
				() -> "quest " + spec.questId() + " claim route is gated on the ritual row");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				claim.afterCommit(),
				() -> "quest " + spec.questId() + " claim row opens the client reward window");
		}
	}

	@Test
	void completionStaysOnTheStartingNpcOnly() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.toList();
			assertFalse(completions.isEmpty(),
				() -> "quest " + spec.questId() + " has completion routes");
			assertEquals(Set.of(spec.handoverNpc()), talkNpcIds(completions),
				() -> "quest " + spec.questId() + " is claimed on the starting NPC only");
			assertTrue(completions.stream()
					.allMatch(route -> "s10".equals(route.sourceNode())),
				() -> "quest " + spec.questId() + " only completes from the claim REWARD row");
		}
	}

	private static Set<Integer> talkNpcIds(List<QuestTransition> routes) {
		Set<Integer> npcIds = new LinkedHashSet<>();
		for (QuestTransition route : routes) {
			if (route.event() instanceof QuestEvent.TalkToNpc talk) {
				npcIds.add(talk.npcId());
			}
		}
		return npcIds;
	}

	@Test
	void staleRewardSavesHealToTheLastRowAndNeverJump() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			CompiledQuestDefinition compiled = definition(spec);
			QuestDefinition definition = compiled.definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + spec.questId() + " must not keep a collapsed started -> reward jump");

			QuestTransition heal = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "s10".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + spec.questId() + " last-row heal edge"));
			assertTrue(heal.conditions().contains(new QuestCondition.StatusIs(QuestStatus.REWARD)),
				() -> "quest " + spec.questId() + " heal only fires in REWARD");
			assertTrue(heal.conditions().contains(new QuestCondition.VariableBelow("var0", FINAL_STEP)),
				() -> "quest " + spec.questId() + " heal covers every migrated reward projection");
			assertTrue(heal.actions().contains(new QuestAction.SetVariable("var0", FINAL_STEP)),
				() -> "quest " + spec.questId() + " heal moves the stale save to the last row");

			for (int oldStep : new int[] {spec.oldReward(), FINAL_STEP - 1}) {
				QuestMutationPlan healed = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, QuestStatus.REWARD, Map.of("var0", oldStep), Map.of()), heal)
					.orElseThrow(() -> new AssertionError(
						"quest " + spec.questId() + " heal plans var0=" + oldStep));
				assertEquals(QuestStatus.REWARD, healed.nextStatus(),
					() -> "quest " + spec.questId() + " healed status");
				assertEquals(FINAL_STEP, unpack(compiled, healed).get("var0"),
					() -> "quest " + spec.questId() + " healed journal step");
			}
		}
	}

	private static Map<Integer, QuestStatus> stepStates(QuestDefinition definition) {
		Map<Integer, QuestStatus> steps = new LinkedHashMap<>();
		for (QuestNode node : definition.nodes()) {
			Integer step = node.projection().variables().get("var0");
			QuestStatus status = node.projection().status();
			if (step == null || status != QuestStatus.START && status != QuestStatus.REWARD) {
				continue;
			}
			steps.put(step, status);
		}
		return steps;
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> Objects.equals(source, route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory);
	}

	private static CompiledQuestDefinition definition(QuestSpec spec) throws IOException {
		try (InputStream input = HolyTemplarFinalRowPairContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + spec.questId() + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + spec.questId() + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestSpec(int questId, int ritualNpc, int ritualItem, int handoverNpc, int oldReward) {
	}
}
