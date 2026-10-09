package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 1345 Bearer Of Bad News 的步号轴与交付分支（QE-054 口径）。
 * 步号轴遵循原版与 legacy 的权威值：和 Kreon 对话后 0 -&gt; 1（原版 FUN_180f7d810
 * SetProgress(0x541, 1)、legacy defaultCloseDialog(env, 0, 1)），扔戒指（use-item）进 REWARD 时
 * 保持 1（原版 FUN_180f03fc0 在 START/step==1 时以 0x100(0x541, 0, 0) 推进、不带步号；
 * legacy useQuestItem(env, item, 1, 2, true, ...) 的 reward 分支不写 nextStep）。
 * 迁移期曾按末行索引把 reward 投影写成 2，交付前客户端任务书步骤整块落空。
 * Locks quest 1345's step axis and turn-in branches (QE-054 caliber).
 * The axis follows the retail/legacy authoritative values: talking to Kreon 0 -&gt; 1 (retail
 * FUN_180f7d810 SetProgress(0x541, 1), legacy defaultCloseDialog(0, 1)) and the ring throw
 * (use-item) entering REWARD keeps 1 (retail FUN_180f03fc0 advances via 0x100(0x541, 0, 0) on
 * START/step==1 without a step value; the reward branch of legacy useQuestItem(1, 2, true) does
 * not write nextStep). The migration wrongly wrote the reward projection as the last-row index 2,
 * blanking the client journal steps before turn-in.
 */
class Quest1345ClientDialogAlignmentTest {
	private static final int DEMOKRITOS_ID = 204006;
	private static final int KREON_ID = 203765;
	private static final int TARNISHED_RING_ID = 182201320;

	@Test
	void keepsTheRetailStepAxisAcrossRingThrowAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "v1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));

		// Kreon 对话：0 -> 1（原版 FUN_180f7d810 SetProgress(0x541, 1)；legacy defaultCloseDialog(0, 1)）。
		// Kreon dialog: 0 -> 1 (retail FUN_180f7d810; legacy defaultCloseDialog(0, 1)).
		QuestTransition talk = transition(definition, "started", "v1",
			new QuestEvent.TalkToNpc(KREON_ID, QuestDialogAction.SETPRO1.id()));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), talk.afterCommit());

		// 扔戒指：直接进 REWARD，步号保持 1（不写轴）；原版 0x100 推进同型。
		// Ring throw: straight into REWARD keeping step 1; retail advances via 0x100 the same way.
		QuestTransition throwRing = transition(definition, "v1", "reward",
			new QuestEvent.UseItem(TARNISHED_RING_ID));
		assertEquals(List.of(), throwRing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), throwRing.afterCommit());

		// 领奖面：Demokritos 对话发 SELECT2。
		// Reward face: the Demokritos dialog opens SELECT2.
		QuestTransition rewardTalk = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(DEMOKRITOS_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			rewardTalk.afterCommit());

		// 无 source 自愈边：迁移期按末行索引写错的存档（REWARD/var0=2）归一为 1。
		// Source-less healing edge: saves persisted at REWARD/var0=2 by the last-row-index writing
		// are normalized to 1 on enter-world.
		List<QuestTransition> heals = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(candidate -> candidate.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 2))))
			.toList();
		assertEquals(1, heals.size(), "quest 1345 healing edge for REWARD/var0=2");
		QuestTransition heal = heals.getFirst();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), heal.actions());
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
		assertEquals(1, routes.size(), "quest 1345 " + source + " -> " + target + " " + event);
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
		try (InputStream input = Quest1345ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/1345.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1345.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
