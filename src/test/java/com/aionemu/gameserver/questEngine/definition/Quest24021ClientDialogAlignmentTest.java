package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 24021 Ghosts in the Desert 的步号轴与撒药分支（QE-054 口径）。
 * 步号轴遵循原版与 legacy 的权威值：接取/对话递进 0 -&gt; 1 -&gt; 2 -&gt; 3 -&gt; 4
 * （原版选项 10003 = SETPRO4 的 FUN_180faea90 显式 SetProgress(0x5dd5, 4)；legacy
 * defaultCloseDialog(env, 3, 4, 182215363, ...)），撒 24021c（use-item + 区域
 * DF2_ITEMUSEAREA_Q2032）进 REWARD 时保持 4（原版 FUN_180f00500 以 0x100(0x5dd5, 0, 0) 推进；
 * legacy useQuestItem(env, item, 4, 4, true, 88) 的 reward 分支不写 nextStep）。
 * 领奖行批次（7a7d27809）曾按末行索引把领奖投影抬到 5 并把自愈边写成 4 -&gt; 5（会改坏正确存档）。
 * Locks quest 24021's step axis and scatter branch (QE-054 caliber).
 * The axis follows the retail/legacy authoritative values: the dialog chain 0 -&gt; 1 -&gt; 2 -&gt; 3 -&gt; 4
 * (retail option 10003 = SETPRO4 in FUN_180faea90 writes SetProgress(0x5dd5, 4) explicitly;
 * legacy defaultCloseDialog(3, 4, 182215363, ...)), and spreading 24021c (use-item in zone
 * DF2_ITEMUSEAREA_Q2032) entering REWARD keeps 4 (retail FUN_180f00500 advances via
 * 0x100(0x5dd5, 0, 0); the reward branch of legacy useQuestItem(4, 4, true, 88) does not write
 * nextStep). The reward-row batch (7a7d27809) raised the reward projection to the last-row index
 * 5 and wrote the healing edge as 4 -&gt; 5 (which rewrote correct saves).
 */
class Quest24021ClientDialogAlignmentTest {
	private static final int TOFYNIR_ID = 802046;
	private static final int SCATTER_ITEM_ID = 182215363;

	@Test
	void keepsTheRetailStepAxisAcrossScatterAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));

		// Tofynir 给道具并推进 3 -> 4（原版选项 10003=SETPRO4；legacy defaultCloseDialog(3, 4, ...)）。
		// Tofynir hands the item and advances 3 -> 4 (retail option 10003=SETPRO4; legacy
		// defaultCloseDialog(3, 4, ...)).
		QuestTransition handover = transition(definition, "s3", "s4",
			new QuestEvent.TalkToNpc(TOFYNIR_ID, QuestDialogAction.SETPRO4.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), handover.conditions());
		assertEquals(List.of(
			new QuestAction.GiveItem(SCATTER_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 4)), handover.actions());

		// 撒 24021c：进 REWARD，步号保持 4（原版 0x100 推进；legacy reward 分支不写 nextStep）。
		// Scatter: enters REWARD keeping step 4 (retail 0x100 advance; the legacy reward branch
		// writes no nextStep).
		QuestTransition scatter = transition(definition, "s4", "reward",
			new QuestEvent.UseItem(SCATTER_ITEM_ID));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 4),
			new QuestCondition.ZoneIs("DF2_ITEMUSEAREA_Q2032", true)), scatter.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(SCATTER_ITEM_ID, 1)), scatter.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(88, QuestMovieType.CUTSCENE),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			scatter.afterCommit());

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
		assertEquals(1, heals.size(), "quest 24021 healing edge for REWARD/var0=5");
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
		assertEquals(1, routes.size(), "quest 24021 " + source + " -> " + target + " " + event);
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
		try (InputStream input = Quest24021ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/24021.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 24021.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
