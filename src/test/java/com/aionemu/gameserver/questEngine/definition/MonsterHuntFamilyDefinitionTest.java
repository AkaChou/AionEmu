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
 * 三个任务全部由原版网格合成器驱动（XML 已退役）：网格节点标签是 {@code a0..aN}（双槽为 {@code a0b0} 形态），
 * 与旧 XML 的 {@code started}/{@code k1..kN} 不同，因此**绝对定位一律按 (状态, 打包投影) 做**，
 * 不按标签断言；转移自带的端点标签仍可直接查（转移本来就按标签保存端点）。
 * <p>
 * All three owners are retail-driven now: absolute node lookups go by (status, packed projection)
 * because the retail grid labels differ from the legacy XML ones.
 */
class MonsterHuntFamilyDefinitionTest {
	/**
	 * The retail confirm range first/last dialog ids.
	 */
	private static final int FIRST_CONFIRM_ID = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ID = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();


	@Test
	void productionCatalogDoesNotRetainTheLegacyMonsterHuntOwners() {
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

	/**
	 * 转移端点按标签查（转移本来就按标签保存端点）。 / Transition endpoints are stored by label.
	 */
	private static Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	/**
	 * REWARD 态领奖节点的投影（应当唯一）。 / Projection of the unique REWARD node.
	 */
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

	/**
	 * START 态网格节点的 var0 取值流（原版网格为 0..N 全段）。 / START-node var0 stream.
	 */
	private static IntStream startVar0s(CompiledQuestDefinition compiled) {
		return compiled.definition().nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START)
			.map(node -> node.projection().variables().getOrDefault("var0", 0))
			.mapToInt(Integer::intValue);
	}

	/**
	 * 原版领奖确认段：dialogId → 动作序列（同一 dialogId 只允许一条）。 / confirm routes keyed by dialog id.
	 */
	private static Map<Integer, List<QuestAction>> completionActions(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete"))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.collect(Collectors.toMap(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId(),
				QuestTransition::actions));
	}

	/**
	 * 确认段全区间（含 NOREWARD 收尾位）。 / The whole confirm range including the NOREWARD slot.
	 */
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

	/**
	 * 固定奖励 + 可选项（可空）+ CompleteQuest 收尾。 / Fixed rewards, optional selectable, completion.
	 */
	private static List<QuestAction> confirm(List<QuestAction> fixed, QuestAction... selectables) {
		List<QuestAction> actions = new ArrayList<>(fixed);
		actions.addAll(List.of(selectables));
		actions.add(new QuestAction.CompleteQuest(0));
		return List.copyOf(actions);
	}

	/**
	 * 生产定义：XML 目录 + 原版 overlay（退役任务不再有 XML，只在 git 历史里）。
	 */
	private CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}

	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
