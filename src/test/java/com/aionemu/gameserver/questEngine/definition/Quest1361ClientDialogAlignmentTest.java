package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 1361 取得饮用水的步号轴与交付分支（QE-054/QE-045 口径）。
 * 步号轴遵循原版与 legacy 的权威值：打水后 0 -&gt; 1（原版 FUN_180f01180 SetProgress(0x551, 1)、
 * legacy setQuestVar(1)），灌满水箱进 REWARD 时保持 1（legacy useQuestObject(env, 1, 1, true) 的
 * reward 分支不写 nextStep；原版 FUN_180f98460 以 0x100(0x551, 0, 0) 推进、不带步号）。
 * 领奖行批次曾按末行索引把 reward 投影误抬为 2，客户端任务书步骤整块空白（2026-10-07 实机报障）。
 * Locks quest 1361's drinking-water step axis and turn-in branches (QE-054/QE-045 caliber).
 * The axis follows the retail/legacy authoritative values: drawing water 0 -&gt; 1 (retail
 * FUN_180f01180 SetProgress(0x551, 1), legacy setQuestVar(1)) and the REWARD state keeps 1
 * (the reward branch of legacy useQuestObject(env, 1, 1, true) does not write nextStep; retail
 * FUN_180f98460 advances via 0x100(0x551, 0, 0) without a step value). The last-row-index batch
 * wrongly raised the reward projection to 2 and blanked the client journal steps (live report
 * 2026-10-07).
 */
class Quest1361ClientDialogAlignmentTest {
	private static final int TURIEL_ID = 203943;
	private static final int WATER_TANK_ID = 700173;
	private static final int EMPTY_BUCKET_ID = 182201326;
	private static final int FILLED_BUCKET_ID = 182201327;

	@Test
	void keepsTheRetailStepAxisAcrossDrawFillAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "v1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));

		// 打水：空桶 -> 水罐，步号 0 -> 1；原版 FUN_180f01180 在 START/step==0 时推进。
		// Drawing water: empty bucket -> filled bucket, step 0 -> 1; retail FUN_180f01180 advances
		// on START/step==0.
		QuestTransition draw = transition(definition, "started", "v1",
			new QuestEvent.ItemPlay(EMPTY_BUCKET_ID, 3000));
		assertEquals(List.of(
			new QuestAction.RemoveItem(EMPTY_BUCKET_ID, 1),
			new QuestAction.GiveItem(FILLED_BUCKET_ID, 1)), draw.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			draw.afterCommit());

		// 灌水箱：持有水罐时进 REWARD，步号保持 1（legacy useQuestObject(1, 1, true)）。
		// Filling the tank: entering REWARD while holding the filled bucket, the step stays 1
		// (legacy useQuestObject(1, 1, true)).
		QuestTransition fill = transition(definition, "v1", "reward",
			new QuestEvent.TalkToNpc(WATER_TANK_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new QuestCondition.HasItem(FILLED_BUCKET_ID, 1)), fill.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(FILLED_BUCKET_ID, 1)), fill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), fill.afterCommit());

		// 领奖面：客户端契约声明页 1352（select2），沿用 legacy sendQuestDialog(env, 1352)。
		// Reward face: the client contract declares page 1352 (select2), matching the legacy
		// sendQuestDialog(env, 1352).
		QuestTransition rewardTalk = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(TURIEL_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			rewardTalk.afterCommit());

		// 无 source 自愈边：领奖行批次把 reward 投影误抬到 2 的存档归一为 1。
		// Source-less healing edge: saves persisted at REWARD/var0=2 by the last-row-index batch
		// are normalized to 1 on enter-world.
		List<QuestTransition> heals = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(candidate -> candidate.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 2))))
			.toList();
		assertEquals(1, heals.size(), "quest 1361 healing edge for REWARD/var0=2");
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
		assertEquals(1, routes.size(), "quest 1361 " + source + " -> " + target + " " + event);
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
		try (InputStream input = Quest1361ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/1361.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1361.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
