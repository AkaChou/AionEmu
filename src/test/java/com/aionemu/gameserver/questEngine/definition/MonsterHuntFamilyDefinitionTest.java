package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full vertical proof for the current MonsterHunt owners 1112 / 1113 / 1120.
 * <p>
 * 三个任务全部由真端网格合成器驱动（XML 已退役）：网格节点标签是 {@code a0..aN}（双槽为 {@code a0b0} 形态），
 * 与旧 XML 的 {@code started}/{@code k1..kN} 不同，因此**绝对定位一律按 (状态, 打包投影) 做**，
 * 不按标签断言；转移自带的端点标签仍可直接查（转移本来就按标签保存端点）。
 * <p>
 * All three owners are retail-driven now: absolute node lookups go by (status, packed projection)
 * because the retail grid labels differ from the legacy XML ones.
 */
class MonsterHuntFamilyDefinitionTest {
	/** 真端领奖确认段首尾 = {@code SELECTED_QUEST_REWARD1(8)..SELECTED_QUEST_NOREWARD(23)}。 */
	/** The retail confirm range first/last dialog ids. */
	private static final int FIRST_CONFIRM_ID = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ID = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	@Test
	void packagedProductionDirectoryCompilesTheThreeHuntOwners() throws Exception {
		QuestCatalog catalog = ProductionQuestDefinitions.catalog();
		// 1112 已迁到真端驱动（XML 退役、定义由 overlay 提供）：判据统一落在生产视图。
		// 1112 is retail-driven now; every owner is present in the production view.
		assertTrue(catalog.find(1112).isPresent());
		assertTrue(catalog.find(1113).isPresent());
		assertTrue(catalog.find(1120).isPresent());
	}

	@Test
	void mushroomThievesKillChainAdvancesVar0OneStepPerKill() throws Exception {
		CompiledQuestDefinition compiled = definition(1113);
		assertEquals(Set.of(210262, 210675), killNpcIds(compiled));
		List<QuestTransition> kills = kills(compiled);
		assertEquals(16, kills.size());
		// started -> k1 -> ... -> k8: each step advances var0 by exactly one.
		for (QuestTransition kill : kills) {
			int source = varsOf(compiled, kill.sourceNode()).get("var0");
			int target = varsOf(compiled, kill.targetNode()).get("var0");
			assertEquals(source + 1, target, kill.sourceNode() + " must advance one kill");
		}
		// The grid is a single serial chain (only one count dimension), so every
		// non-final node has exactly two outgoing kill routes (one per npc id).
		Map<String, Long> outgoing = kills.stream()
			.collect(Collectors.groupingBy(QuestTransition::sourceNode, Collectors.counting()));
		assertEquals(Set.of(2L), outgoing.values().stream().collect(Collectors.toSet()));
		// 网格计数段 = 0..8：零段节点是接取落点，8 段节点是满计数（报告入口）。
		// The grid counter spans 0..8: the zero node takes the accept and 8 is the saturated report row.
		assertEquals(0, gridMinVar0(compiled));
		assertEquals(8, gridMaxVar0(compiled));
	}

	@Test
	void thinningWorgsKillChainAdvancesVar0OneStepPerKill() throws Exception {
		CompiledQuestDefinition compiled = definition(1120);
		assertEquals(Set.of(210142, 210143), killNpcIds(compiled));
		assertEquals(18, kills(compiled).size());
		for (QuestTransition kill : kills(compiled)) {
			int source = varsOf(compiled, kill.sourceNode()).get("var0");
			int target = varsOf(compiled, kill.targetNode()).get("var0");
			assertEquals(source + 1, target, kill.sourceNode() + " must advance one kill");
		}
		assertEquals(9, gridMaxVar0(compiled));
	}

	@Test
	void toFishInPeaceUsesTwoIndependentSixBitKillCounts() throws Exception {
		CompiledQuestDefinition compiled = definition(1112);
		assertEquals(Set.of(210259, 210260, 210065, 210066), killNpcIds(compiled));
		List<QuestTransition> kills = kills(compiled);
		assertEquals(120, kills.size());

		// var0 advances only when a water-target npc (210259/210260) dies.
		List<QuestTransition> aKills = kills.stream()
			.filter(k -> Set.of(210259, 210260).contains(((QuestEvent.KillNpc) k.event()).npcId())).toList();
		assertEquals(60, aKills.size());
		for (QuestTransition kill : aKills) {
			Map<String, Integer> source = varsOf(compiled, kill.sourceNode());
			Map<String, Integer> target = varsOf(compiled, kill.targetNode());
			assertEquals(source.get("var0") + 1, target.get("var0"), "var0 must advance on water kill");
			assertEquals(source.get("var1"), target.get("var1"), "var1 must not move on water kill");
		}
		// var1 advances only when a different target (210065/210066) dies.
		List<QuestTransition> bKills = kills.stream()
			.filter(k -> Set.of(210065, 210066).contains(((QuestEvent.KillNpc) k.event()).npcId())).toList();
		assertEquals(60, bKills.size());
		for (QuestTransition kill : bKills) {
			Map<String, Integer> source = varsOf(compiled, kill.sourceNode());
			Map<String, Integer> target = varsOf(compiled, kill.targetNode());
			assertEquals(source.get("var1") + 1, target.get("var1"), "var1 must advance on fishing kill");
			assertEquals(source.get("var0"), target.get("var0"), "var0 must not move on fishing kill");
		}
		// The acceptance gate requires BOTH counts saturated.
		Map<String, Integer> rewardVars = rewardVars(compiled);
		assertEquals(5, rewardVars.get("var0"));
		assertEquals(5, rewardVars.get("var1"));
	}

	@Test
	void completionRewardsCarrySelectableItemsAndFixedRewards() throws Exception {
		CompiledQuestDefinition mushroom = definition(1113);
		Map<Integer, List<QuestAction>> mushroomOptions = completionActions(mushroom);
		assertEquals(completionIds(2), mushroomOptions.keySet());
		List<QuestAction> mushroomFixed = List.of(
			new QuestAction.GrantReward("EXP", 0, 1738, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 160003001, 3));
		assertEquals(confirm(mushroomFixed, new QuestAction.GrantReward("ITEM", 162000048, 1)),
			mushroomOptions.get(FIRST_CONFIRM_ID));
		assertEquals(confirm(mushroomFixed, new QuestAction.GrantReward("ITEM", 169000003, 150)),
			mushroomOptions.get(FIRST_CONFIRM_ID + 1));
		// 窗口自动确认通道（全局奖励窗按钮 110+k）：第 k 个可选槽与手动确认同构。
		// Window auto-confirm (global reward-window buttons 110+k): slot k mirrors the manual confirm.
		assertEquals(confirm(mushroomFixed, new QuestAction.GrantReward("ITEM", 162000048, 1)),
			mushroomOptions.get(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()));
		assertEquals(confirm(mushroomFixed, new QuestAction.GrantReward("ITEM", 169000003, 150)),
			mushroomOptions.get(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + 1));
		// XML 只声明了 2 条 <choice>：其余确认 id（含 NOREWARD 收尾位）只发固定奖励。
		// Only two <choice> rows were declared, so the remaining confirm ids carry fixed rewards alone.
		for (int dialogId = FIRST_CONFIRM_ID + 2; dialogId <= LAST_CONFIRM_ID; dialogId++) {
			assertEquals(confirm(mushroomFixed), mushroomOptions.get(dialogId), "dialogId " + dialogId);
		}

		CompiledQuestDefinition worgs = definition(1120);
		Map<Integer, List<QuestAction>> worgsOptions = completionActions(worgs);
		assertEquals(completionIds(2), worgsOptions.keySet());
		List<QuestAction> worgsFixed = List.of(
			new QuestAction.GrantReward("GOLD", 0, 3040, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 4455, QuestRewardAmountMode.QUEST_BASE));
		assertEquals(confirm(worgsFixed, new QuestAction.GrantReward("ITEM", 162000048, 1)),
			worgsOptions.get(FIRST_CONFIRM_ID));
		assertEquals(confirm(worgsFixed, new QuestAction.GrantReward("ITEM", 169000003, 250)),
			worgsOptions.get(FIRST_CONFIRM_ID + 1));
		assertEquals(confirm(worgsFixed), worgsOptions.get(LAST_CONFIRM_ID));
		// 窗口自动确认通道与手动确认同构（1113 同款）。 / Window auto-confirm mirrors the manual confirm.
		assertEquals(confirm(worgsFixed, new QuestAction.GrantReward("ITEM", 162000048, 1)),
			worgsOptions.get(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()));
		assertEquals(confirm(worgsFixed, new QuestAction.GrantReward("ITEM", 169000003, 250)),
			worgsOptions.get(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + 1));

		CompiledQuestDefinition fishing = definition(1112);
		// 无 <choice> 可选项：确认段与固定奖励位 108 的动作序列完全同构。
		// No selectable: the confirm range and the fixed-only 108 route all match one shape.
		Map<Integer, List<QuestAction>> fishingCompletions = completionActions(fishing);
		assertEquals(completionIds(0), fishingCompletions.keySet());
		List<QuestAction> expectedFishing = confirm(List.of(
			new QuestAction.GrantReward("GOLD", 0, 1810, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 1375, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 169300002, 30)));
		for (Map.Entry<Integer, List<QuestAction>> path : fishingCompletions.entrySet()) {
			assertEquals(expectedFishing, path.getValue(), "dialogId " + path.getKey());
		}

		// 奖励池顺序取真端 quest.xml 的 reward group 声明序（真端为形状权威，内容与旧 XML 一致）。
		// The pool order follows the retail reward group declaration; the contents match the legacy XML.
		assertEquals(List.of(new QuestReward("ITEM", 160003001, 3L),
			new QuestReward("SELECTABLE_ITEM", 162000048, 1L),
			new QuestReward("SELECTABLE_ITEM", 169000003, 150L)),
			mushroom.definition().metadata().rewards().stream()
				.filter(r -> !r.kind().equals("EXP")).toList());
	}

	@Test
	void productionCatalogDoesNotRetainTheLegacyMonsterHuntOwners() throws Exception {
		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");
	}

	private static List<QuestTransition> kills(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc).toList();
	}

	private static Set<Integer> killNpcIds(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.map(QuestTransition::event).filter(e -> e instanceof QuestEvent.KillNpc)
			.map(e -> ((QuestEvent.KillNpc) e).npcId()).collect(Collectors.toSet());
	}

	/** 转移端点按标签查（转移本来就按标签保存端点）。 / Transition endpoints are stored by label. */
	private static Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	/** REWARD 态领奖节点的投影（应当唯一）。 / Projection of the unique REWARD node. */
	private static Map<String, Integer> rewardVars(CompiledQuestDefinition compiled) {
		List<QuestNode> rewards = compiled.definition().nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD).toList();
		assertEquals(1, rewards.size(), "领奖节点应当唯一");
		return rewards.getFirst().projection().variables();
	}

	private static int gridMinVar0(CompiledQuestDefinition compiled) {
		return startVar0s(compiled).min().orElseThrow();
	}

	private static int gridMaxVar0(CompiledQuestDefinition compiled) {
		return startVar0s(compiled).max().orElseThrow();
	}

	/** START 态网格节点的 var0 取值流（真端网格为 0..N 全段）。 / START-node var0 stream. */
	private static IntStream startVar0s(CompiledQuestDefinition compiled) {
		return compiled.definition().nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START)
			.map(node -> node.projection().variables().getOrDefault("var0", 0))
			.mapToInt(Integer::intValue);
	}

	/** 真端领奖确认段：dialogId → 动作序列（同一 dialogId 只允许一条）。 / confirm routes keyed by dialog id. */
	private static Map<Integer, List<QuestAction>> completionActions(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete"))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.collect(Collectors.toMap(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId(),
				QuestTransition::actions));
	}

	/** 确认段全区间（含 NOREWARD 收尾位）。 / The whole confirm range including the NOREWARD slot. */
	private static Set<Integer> confirmRange() {
		return IntStream.rangeClosed(FIRST_CONFIRM_ID, LAST_CONFIRM_ID).boxed().collect(Collectors.toSet());
	}

	/**
	 * 确认段 + 奖励窗自动确认位：有可选时第 k 槽挂 110+k（SELECTED_QUEST_AUTO_REWARD1+i），
	 * 无可选时固定奖励挂 108（SELECTED_QUEST_AUTO_REWARD）。
	 * Confirm range plus the reward-window auto-confirm ids: 110+k per selectable slot,
	 * or the fixed-only 108 route when no selectable exists.
	 */
	private static Set<Integer> completionIds(int selectableSlots) {
		Set<Integer> ids = confirmRange();
		if (selectableSlots == 0) {
			ids.add(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id());
		} else {
			for (int slot = 0; slot < selectableSlots; slot++) {
				ids.add(QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + slot);
			}
		}
		return ids;
	}

	/** 固定奖励 + 可选项（可空）+ CompleteQuest 收尾。 / Fixed rewards, optional selectable, completion. */
	private static List<QuestAction> confirm(List<QuestAction> fixed, QuestAction... selectables) {
		List<QuestAction> actions = new ArrayList<>(fixed);
		actions.addAll(List.of(selectables));
		actions.add(new QuestAction.CompleteQuest(0));
		return List.copyOf(actions);
	}

	/** 生产定义：XML 目录 + 真端 overlay（退役任务不再有 XML，只在 git 历史里）。 */
	private CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}

	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
