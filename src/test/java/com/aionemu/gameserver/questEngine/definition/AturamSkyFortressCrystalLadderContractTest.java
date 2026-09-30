package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
 * 锁定批次 52：18301/28301（阿图拉姆空中要塞「监视水晶球」双子）的行阶梯。
 * <p>
 * 判据：客户端 {@code quest_q18301.html}/{@code quest_q28301.html} 的 {@code quest_summary} 各 3 行
 * （可见槽位 0/3/6）——行 0 = 破坏兵站区域的 7 个监视水晶球（计数显示 {@code ([%2]/7)}）、
 * 行 1 = 击毁胡根后取得 H-Core（胡根动力装置）、行 2 = 把动力装置交给 Hariken 领奖。
 * 客户端 {@code quest_script_monster.csv} 对 {@code idstation_fobj_dr_cm_01} 声明 {@code Progress(0~6)}，
 * 即七个水晶球占 step 0..6、第七个把 SECTION_0 推到 7；迁移前 handler
 * {@code _18301MyPrec_H_ious}/{@code _28301Power_On} 走的正是这条阶梯
 * （水晶球 var&lt;7 每次 +1，var==7 才在 H-Core 处取装置并置 REWARD）。天族侧 18301 被塌陷成
 * {@code started -> reward} 且 reward 投影停在 0，行 1/2 永远不亮（审计
 * {@code ROW_BEHIND | MISSING_TAIL_ROWS | ROW_WITHOUT_STATE}）；魔族镜像 28301 与同副本 18302/18314
 * 早已是「计数阶梯 + 满计数领奖」形态，本批把 18301 收敛到同形。
 * <p>
 * Locks batch 52: both halves of the Aturam Sky Fortress spy-crystal pair keep the seven-step crystal
 * ladder (client {@code Progress(0~6)}), grant the faction work item when the H-Core is taken at step 7,
 * claim the reward on Hariken only, and heal the migrated REWARD/var0=0 saves to step 7.
 */
class AturamSkyFortressCrystalLadderContractTest {

	private static final int REWARD_STEP = 7;
	/** 普通副本 702656 与活动副本 730373 是同一种监视水晶球（同族 18314/28314 也是这个 id 组合）。 */
	private static final Set<Integer> CRYSTALS = Set.of(702656, 730373);
	private static final int H_CORE = 730374;
	private static final int HARIKEN = 799530;
	private static final QuestSpec ELYOS = new QuestSpec(18301, 182212100);
	private static final QuestSpec ASMODIANS = new QuestSpec(28301, 182212110);

	@Test
	void bothHalvesShareTheSevenCrystalCounterLadder() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			Map<Integer, QuestStatus> steps = stepStates(definition);
			for (int step = 0; step <= REWARD_STEP; step++) {
				final int current = step;
				assertEquals(current == REWARD_STEP ? QuestStatus.REWARD : QuestStatus.START, steps.get(current),
					() -> "quest " + spec.questId() + " step " + current + " must own a journal state");
			}
			assertEquals(REWARD_STEP, node(definition, "reward").projection().variables().get("var0"),
				() -> "quest " + spec.questId() + " reward projection is the seventh crystal step");
			assertEquals(Map.of("var0", REWARD_STEP),
				definition.progressLayout().unpack(definition.progressLayout().pack(Map.of("var0", REWARD_STEP))),
				() -> "quest " + spec.questId() + " reward step fits its declared SECTION_0 bit field");

			String[] sources = {"started", "k1", "k2", "k3", "k4", "k5", "k6"};
			String[] targets = {"k1", "k2", "k3", "k4", "k5", "k6", "k7"};
			for (int index = 0; index < sources.length; index++) {
				final int step = index;
				List<QuestTransition> kills = routes(definition, sources[index], targets[index]);
				assertEquals(1, kills.size(),
					() -> "quest " + spec.questId() + " crystal step " + step + " is a single route");
				QuestTransition route = kills.getFirst();
				assertEquals(new QuestEvent.KillNpcSet(CRYSTALS), route.event(),
					() -> "quest " + spec.questId() + " counts both spy-crystal object ids");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					route.afterCommit(),
					() -> "quest " + spec.questId() + " crystal step " + step + " only refreshes the counter");
			}
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + spec.questId() + " must not keep a collapsed started -> reward jump");
		}
	}

	@Test
	void crystalsNeverPushTheCounterPastTheSeventhStep() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			CompiledQuestDefinition compiled = definition(spec);
			QuestEvent kill = new QuestEvent.KillNpcSet(CRYSTALS);
			assertTrue(plansFor(compiled, kill, QuestStatus.START, Map.of("var0", REWARD_STEP)).isEmpty(),
				() -> "quest " + spec.questId() + " step 7 is the saturated crystal count");
			assertTrue(plansFor(compiled, kill, QuestStatus.REWARD, Map.of("var0", REWARD_STEP)).isEmpty(),
				() -> "quest " + spec.questId() + " REWARD save ignores further crystal kills");
			assertEquals(1, plansFor(compiled, kill, QuestStatus.START, Map.of("var0", 0)).size(),
				() -> "quest " + spec.questId() + " first crystal advances the counter");
		}
	}

	@Test
	void hCorePickupOnTheSeventhStepGrantsTheFactionWorkItem() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			CompiledQuestDefinition compiled = definition(spec);
			QuestDefinition definition = compiled.definition();
			QuestTransition pickup = singleRoute(definition, "k7", "reward", H_CORE,
				QuestDialogAction.SETPRO2.id());
			assertEquals("k7", pickup.sourceNode(), () -> "quest " + spec.questId() + " pickup source row");
			assertEquals(List.of(new QuestAction.GiveItem(spec.workItem(), 1)), pickup.actions(),
				() -> "quest " + spec.questId() + " grants its own work item when the H-Core is taken");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.CloseDialog()),
				pickup.afterCommit(),
				() -> "quest " + spec.questId() + " refreshes the journal after taking the device");

			List<QuestMutationPlan> planned = plansFor(compiled,
				new QuestEvent.TalkToNpc(H_CORE, QuestDialogAction.SETPRO2.id()), QuestStatus.START,
				Map.of("var0", REWARD_STEP));
			assertEquals(1, planned.size(),
				() -> "quest " + spec.questId() + " H-Core pickup plans from k7 only");
			assertEquals(QuestStatus.REWARD, planned.getFirst().nextStatus(),
				() -> "quest " + spec.questId() + " H-Core pickup enters REWARD");
			assertEquals(REWARD_STEP, unpack(compiled, planned.getFirst()).get("var0"),
				() -> "quest " + spec.questId() + " H-Core pickup keeps the last crystal step");
			assertTrue(planned.getFirst().requiredActions()
					.contains(new QuestAction.GiveItem(spec.workItem(), 1)),
				() -> "quest " + spec.questId() + " planned pickup grants the work item");

			assertTrue(plansFor(compiled, new QuestEvent.TalkToNpc(H_CORE, QuestDialogAction.SETPRO2.id()),
					QuestStatus.START, Map.of("var0", REWARD_STEP - 1)).isEmpty(),
				() -> "quest " + spec.questId() + " never grants the device before the crystals are gone");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				singleRoute(definition, "k7", "k7", H_CORE, QuestDialogAction.USE_OBJECT.id()).afterCommit(),
				() -> "quest " + spec.questId() + " row 1 opens select2(1352)");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
				singleRoute(definition, "k7", "k7", H_CORE, QuestDialogAction.SELECT2_1.id()).afterCommit(),
				() -> "quest " + spec.questId() + " row 1 opens select2_1(1353)");
		}
	}

	@Test
	void claimRowIsHarikenOnlyAndOpensTheRewardWindow() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			QuestDefinition definition = definition(spec).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.toList();
			assertFalse(completions.isEmpty(),
				() -> "quest " + spec.questId() + " has a completion route");
			assertEquals(Set.of(HARIKEN), talkNpcIds(completions),
				() -> "quest " + spec.questId() + " is claimed on Hariken only");
			assertTrue(completions.stream().allMatch(route -> "reward".equals(route.sourceNode())),
				() -> "quest " + spec.questId() + " only completes from the claim REWARD row");
			// 领奖行先开 select_success(10002)，其 1009 按钮由 npc-complete 的 preview 路由承接：
			// USE_OBJECT / SELECT_QUEST_REWARD 各一条，都落在 tier 0 的奖励窗（页 5）。
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.DEFAULT_SUCCESS.id())),
				singleRoute(definition, "reward", "reward", HARIKEN,
					QuestDialogAction.QUEST_SELECT.id()).afterCommit(),
				() -> "quest " + spec.questId() + " claim row keeps select_success(10002)");
			for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
					QuestDialogAction.SELECT_QUEST_REWARD)) {
				QuestTransition route = singleRoute(definition, "reward", "reward", HARIKEN, preview.id());
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), route.afterCommit(),
					() -> "quest " + spec.questId() + " preview " + preview + " opens the tier-0 window");
			}
		}
	}

	@Test
	void staleRewardSavesHealToTheSeventhStep() throws Exception {
		for (QuestSpec spec : List.of(ELYOS, ASMODIANS)) {
			assertRewardHeal(spec);
		}
	}

	private static void assertRewardHeal(QuestSpec spec) throws IOException {
		CompiledQuestDefinition compiled = definition(spec);
		QuestDefinition definition = compiled.definition();
		QuestTransition heal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "reward".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.findFirst().orElseThrow(() -> new AssertionError("quest 18301 reward heal edge"));
		assertTrue(heal.conditions().contains(new QuestCondition.StatusIs(QuestStatus.REWARD)),
			() -> "quest " + spec.questId() + " heal only fires in REWARD");
		assertTrue(heal.conditions().contains(new QuestCondition.VariableBelow("var0", REWARD_STEP)),
			() -> "quest " + spec.questId() + " heal covers the migrated var0=0 projection");
		assertTrue(heal.actions().contains(new QuestAction.SetVariable("var0", REWARD_STEP)),
			() -> "quest " + spec.questId() + " heal moves the stale save to the seventh crystal step");
		QuestMutationPlan healed = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 0), Map.of()), heal).orElseThrow();
		assertEquals(QuestStatus.REWARD, healed.nextStatus(), () -> "quest " + spec.questId() + " healed status");
		assertEquals(REWARD_STEP, unpack(compiled, healed).get("var0"),
			() -> "quest " + spec.questId() + " healed journal step");
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

	private static QuestTransition singleRoute(QuestDefinition definition, String source, String target,
			int npcId, int actionId) {
		List<QuestTransition> matches = routes(definition, source, target).stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(npcId, actionId)))
			.toList();
		assertEquals(1, matches.size(), () -> "route " + source + " -> " + target + " on npc " + npcId
			+ " action " + actionId);
		return matches.getFirst();
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

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> Objects.equals(source, route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static List<QuestMutationPlan> plansFor(CompiledQuestDefinition compiled, QuestEvent event,
			QuestStatus status, Map<String, Integer> variables) {
		QuestSnapshot snap = snapshot(compiled, status, variables, Map.of());
		List<QuestMutationPlan> plans = new ArrayList<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			QuestMutationPlanner.plan(compiled, snap, event, transition).ifPresent(plans::add);
		}
		return plans;
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
		try (InputStream input = AturamSkyFortressCrystalLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest/definitions/quests/" + spec.questId() + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + spec.questId() + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestSpec(int questId, int workItem) {
	}
}
