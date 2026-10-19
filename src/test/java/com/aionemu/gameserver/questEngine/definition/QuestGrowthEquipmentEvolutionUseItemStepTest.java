package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成长链「装备进化 1/2」的推进步必须是**使用**石头，而不是**获得**石头。
 * <p>客户端任务页（19678/19679 的 html）写的是「使用 XXX 尝试强化/觉醒 1 次」，客户端发的是
 * 强化窗口 / 物品使用事件；生成器 {@code generate_item_collecting_chains.py} 曾把 metadata
 * {@code <items>} 当成收集上报物，产出 {@code <get-item>}，于是玩家用掉石头后任务仍停在首步。
 * 同一缺陷类：事件的物品恰是本任务自己的 ITEM 奖励——「获得奖励物才能推进」在语义上不可能成立。
 * <p>
 * The growth chain's progress step must fire on <b>using</b> the stone, not on obtaining it: the
 * client page says "use it once" and the client reports an item-use event, while the generator
 * emitted {@code <get-item>} from the metadata {@code <items>} block. Family-wide invariant: a
 * quest step keyed on an item the quest itself rewards cannot be an obtain event.
 */
class QuestGrowthEquipmentEvolutionUseItemStepTest {
	private static final int OMEGA_ENCHANTMENT_STONE = 166020000;
	private static final int TEMPERING_SOLUTION = 166030005;

	@Test
	void growthStepsAdvanceOnUseAndNeverOnObtain() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (int[] contract : new int[][] {
			{ 19678, OMEGA_ENCHANTMENT_STONE }, { 19679, TEMPERING_SOLUTION },
			{ 29678, OMEGA_ENCHANTMENT_STONE }, { 29679, TEMPERING_SOLUTION } }) {
			assertContract(catalog.findExecutable(contract[0]).orElseThrow(), contract[1]);
		}
	}

	@Test
	void noQuestGatesAProgressStepOnObtainingItsOwnReward() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> violations = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			for (QuestTransition transition : definition.transitions()) {
				if (transition.event() instanceof QuestEvent.GetItem(int itemId)
						&& definition.metadata().rewards().stream().anyMatch(reward ->
							"ITEM".equals(reward.kind()) && reward.id() == itemId)) {
					violations.add("quest " + definition.id() + " node " + transition.sourceNode()
						+ " advances on obtaining its own reward item " + itemId);
				}
			}
		}
		assertTrue(violations.isEmpty(), () -> "obtain events on own reward items: " + violations);
	}

	@Test
	void enchantWindowStoneUseReachesTheQuestUseEventLane() throws Exception {
		// 强化石走 CM_ENCHANMENT_STONES（真端 User__DoEnchantItem），不经过 CM_USE_ITEM；
		// 该包不投递任务用物事件时，成长任务的「强化 1 次」步在强化窗口里永远不推进。
		// Enchant stones are consumed through CM_ENCHANMENT_STONES (retail User__DoEnchantItem),
		// never CM_USE_ITEM; without the quest hook the growth step can never advance there.
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_ENCHANMENT_STONES.java"));
		assertTrue(source.contains("QuestEngine.getInstance().onItemUseEvent("),
			"enchant-stone use must reach the quest use-item lane");
	}

	private static void assertContract(CompiledQuestDefinition compiled, int stoneId) {
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "s0", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		List<QuestTransition> useRoutes = definition.transitions().stream()
			.filter(candidate -> candidate.event().equals(new QuestEvent.UseItem(stoneId)))
			.toList();
		assertEquals(1, useRoutes.size(), () -> "quest " + definition.id() + " use-item route for " + stoneId);
		QuestTransition use = useRoutes.getFirst();
		assertEquals("s0", use.sourceNode());
		assertEquals("s1", use.targetNode());
		assertEquals(List.of(), use.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), use.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			use.afterCommit());

		assertFalse(definition.transitions().stream()
			.anyMatch(candidate -> candidate.event().equals(new QuestEvent.GetItem(stoneId))),
			() -> "quest " + definition.id() + " must not advance on obtaining the stone");

		// 运行时：s0 上用石头推进到 s1，再用在 s1 上不应重复推进（源节点变量不符）。
		// Runtime: use advances s0 -> s1, and a second use no longer matches from s1.
		QuestSnapshot before = new QuestSnapshot(7, definition.id(), QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, before, use.event(), use).orElseThrow();
		assertEquals(QuestStatus.START, plan.nextStatus());
		assertEquals(Map.of("var0", 1), definition.progressLayout().unpack(plan.nextPackedVariables()));
		QuestSnapshot after = new QuestSnapshot(7, definition.id(), QuestStatus.START,
			plan.nextPackedVariables(), Map.of());
		assertTrue(QuestMutationPlanner.plan(compiled, after, use.event(), use).isEmpty());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}
}
