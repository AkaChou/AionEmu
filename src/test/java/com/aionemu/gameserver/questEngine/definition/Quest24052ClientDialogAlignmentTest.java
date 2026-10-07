package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 24052 A Frozen City 的步号轴与击杀分支（QE-054 口径）。
 * 步号轴遵循 legacy 的权威值：三个道具依序使用 1 -&gt; 2 -&gt; 3 -&gt; 4
 * （legacy onItemUseEvent 在 var 1/2/3 时 setQuestVarById(0, var + 1)，任一在场道具均推进一步），
 * 击杀寄生冰灵 233864 后进 REWARD 保持 4（legacy defaultOnKillEvent(env, 233864, 4, true) 的
 * reward 分支不写 nextStep）。领奖行批次（7a7d27809）仅把领奖投影按末行索引抬到 5 并把自愈边
 * 写成 4 -&gt; 5（会改坏正确存档）；道具递进段本身正确未动。
 * Locks quest 24052's step axis and kill branch (QE-054 caliber).
 * The axis follows the legacy authoritative values: the three items advance 1 -&gt; 2 -&gt; 3 -&gt; 4 in
 * order (legacy onItemUseEvent calls setQuestVarById(0, var + 1) at var 1/2/3; any carried item
 * advances one step), and killing the parasitic ice spirit 233864 entering REWARD keeps 4 (the
 * reward branch of legacy defaultOnKillEvent(env, 233864, 4, true) does not write nextStep). The
 * reward-row batch (7a7d27809) only raised the reward projection to the last-row index 5 and wrote
 * the healing edge as 4 -&gt; 5 (which rewrote correct saves); the item chain itself was correct.
 */
class Quest24052ClientDialogAlignmentTest {
	private static final int PARASITIC_ICE_SPIRIT_ID = 233864;
	private static final int FIRST_ITEM_ID = 182215378;

	@Test
	void keepsTheLegacyStepAxisAcrossItemChainKillAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));

		// 道具递进：任一在场道具在当前阶段推进一步（s1 -> s2，真端同样接受任意道具）。
		// Item chain: any carried item advances one step at the current stage (s1 -> s2; retail
		// accepts any of the three items the same way).
		QuestTransition firstStep = transition(definition, "s1", "s2",
			new QuestEvent.UseItem(FIRST_ITEM_ID));
		assertEquals(List.of(
			new QuestCondition.ZoneIs("DF3_ITEMUSEAREA_Q2056", true),
			new QuestCondition.HasItem(FIRST_ITEM_ID, 1)), firstStep.conditions());
		assertEquals(List.of(
			new QuestAction.BlockDefaultItemUse(),
			new QuestAction.RemoveItem(FIRST_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 2)), firstStep.actions());

		// 击杀寄生冰灵：进 REWARD，步号保持 4（legacy reward 分支不写 nextStep）。
		// Killing the parasitic ice spirit: enters REWARD keeping step 4 (the legacy reward branch
		// writes no nextStep).
		QuestTransition kill = transition(definition, "s4", "reward",
			new QuestEvent.KillNpc(PARASITIC_ICE_SPIRIT_ID));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), kill.afterCommit());

		// 计时器超时：回到起始阶段（legacy onQuestTimerEndEvent var==4 -> 0）。
		// Timer expiry: back to the starting stage (legacy onQuestTimerEndEvent var==4 -> 0).
		QuestTransition timeout = transition(definition, "s4", "s0",
			new QuestEvent.QuestTimerEnd());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0)), timeout.actions());

		// 无 source 自愈边：领奖行批次写的反向边（4 -> 5）已改为归一（REWARD/5 -> 4）。
		// Source-less healing edge: the reward-row batch's reversed edge (4 -> 5) is now the
		// normalization REWARD/5 -> 4.
		List<QuestTransition> heals = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(candidate -> candidate.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 5))))
			.toList();
		assertEquals(1, heals.size(), "quest 24052 healing edge for REWARD/var0=5");
		QuestTransition heal = heals.getFirst();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), heal.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 24052 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest24052ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/24052.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 24052.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
