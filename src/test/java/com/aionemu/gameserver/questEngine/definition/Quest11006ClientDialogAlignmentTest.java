package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 11006 Testing The Waters 的步号轴与两段装水分支（QE-054 口径）。
 * 步号轴遵循真端与 legacy 的权威值：装第一瓶水 0 -&gt; 1（真端 FUN_180f00a50
 * SetProgress(0x2afe, 1)、legacy useQuestItem(env, item, 0, 1, false, ...)），Clodia 对话
 * 1 -&gt; 2（legacy defaultCloseDialog(env, 1, 2)），装第二瓶水进 REWARD 时保持 2
 * （真端 FUN_180f03f20 在 START/step==2 时以 0x100(0x2afe, 0, 0) 推进；legacy
 * useQuestItem(env, item, 2, 2, true, ...) 的 reward 分支不写 nextStep）。
 * 领奖行批次（7a7d27809）曾把领奖投影按末行索引抬到 3 并把自愈边写成 2 -&gt; 3（会改坏正确存档）。
 * Locks quest 11006's step axis and both water-fill branches (QE-054 caliber).
 * The axis follows the retail/legacy authoritative values: first fill 0 -&gt; 1 (retail
 * FUN_180f00a50 SetProgress(0x2afe, 1), legacy useQuestItem(0, 1, false)), the Clodia dialog
 * 1 -&gt; 2 (legacy defaultCloseDialog(1, 2)), and the second fill entering REWARD keeps 2
 * (retail FUN_180f03f20 advances via 0x100(0x2afe, 0, 0) on START/step==2; the reward branch of
 * legacy useQuestItem(2, 2, true) does not write nextStep). The reward-row batch (7a7d27809)
 * raised the reward projection to the last-row index 3 and wrote the healing edge as 2 -&gt; 3
 * (which rewrote correct saves).
 */
class Quest11006ClientDialogAlignmentTest {
	private static final int CLODIA_ID = 798940;
	private static final int EMPTY_BOTTLE_ID = 182206704;
	private static final int FIRST_SAMPLE_ID = 182206706;
	private static final int SECOND_BOTTLE_ID = 182206705;
	private static final int SECOND_SAMPLE_ID = 182206707;

	@Test
	void keepsTheRetailStepAxisAcrossBothFillsAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "v1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "v2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2));

		// 装第一瓶水：0 -> 1（真端 SetProgress(0x2afe, 1)；legacy useQuestItem(0, 1, false)）。
		// First fill: 0 -> 1 (retail SetProgress(0x2afe, 1); legacy useQuestItem(0, 1, false)).
		QuestTransition firstFill = transition(definition, "started", "v1",
			new QuestEvent.UseItem(EMPTY_BOTTLE_ID));
		assertEquals(List.of(
			new QuestAction.RemoveItem(EMPTY_BOTTLE_ID, 1),
			new QuestAction.GiveItem(FIRST_SAMPLE_ID, 1)), firstFill.actions());

		// Clodia 对话：1 -> 2（legacy defaultCloseDialog(1, 2)）。
		// Clodia dialog: 1 -> 2 (legacy defaultCloseDialog(1, 2)).
		QuestTransition talk = transition(definition, "v1", "v2",
			new QuestEvent.TalkToNpc(CLODIA_ID, QuestDialogAction.SETPRO2.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), talk.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(FIRST_SAMPLE_ID, 1),
			new QuestAction.GiveItem(SECOND_BOTTLE_ID, 1)), talk.actions());

		// 装第二瓶水：进 REWARD，步号保持 2（真端 0x100 推进；legacy reward 分支不写 nextStep）。
		// 真端取证（ScriptDLL64 FUN_180f03f20，注册于 0xadc40f1）：只做 0x100 推进 + 0x2f8 通告，零发页；
		// 旧的页 10 尾随是翻译夸大（用物没有对话对象，下发 SM_DIALOG_WINDOW(0, 10) 即 load fail），已删。
		// Second fill: enters REWARD keeping step 2 (retail 0x100 advance; the legacy reward branch
		// writes no nextStep). Retail evidence (ScriptDLL64 FUN_180f03f20, registered at 0xadc40f1):
		// only a 0x100 advance plus a 0x2f8 notice, no page; the old trailing page 10 was translation
		// exaggeration (an item use has no dialog object, so SM_DIALOG_WINDOW(0, 10) fails to load) and is removed.
		QuestTransition secondFill = transition(definition, "v2", "reward",
			new QuestEvent.UseItem(SECOND_BOTTLE_ID));
		assertEquals(List.of(
			new QuestAction.RemoveItem(SECOND_BOTTLE_ID, 1),
			new QuestAction.GiveItem(SECOND_SAMPLE_ID, 1)), secondFill.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			secondFill.afterCommit());

		// 无 source 自愈边：领奖行批次写的反向边（2 -> 3）已改为归一（REWARD/3 -> 2）。
		// Source-less healing edge: the reward-row batch's reversed edge (2 -> 3) is now the
		// normalization REWARD/3 -> 2.
		List<QuestTransition> heals = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(candidate -> candidate.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 3))))
			.toList();
		assertEquals(1, heals.size(), "quest 11006 healing edge for REWARD/var0=3");
		QuestTransition heal = heals.getFirst();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), heal.actions());
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
		assertEquals(1, routes.size(), "quest 11006 " + source + " -> " + target + " " + event);
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
		try (InputStream input = Quest11006ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/11006.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 11006.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
