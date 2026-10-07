package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 4338 The Cursed Necklace 的步号轴与焚毁项链分支（QE-054 口径）。
 * 步号轴遵循真端：选向导（SETPRO5）显式 4 -&gt; 5（FUN_180fb8390/FUN_180fb84d0 的
 * 0x110(0x10f2, 4, 5, ...)），把项链扔进岩浆（use-item）直接进 REWARD 且轴保持 5
 * （FUN_180f00820 在 START/step==5 时以 0x100(0x10f2, 0, 0) 推进）。
 * 错误批次（caf547879）曾写成推进到 6、汇报对话再抬到 7，与真端冲突；s6 仅作为旧存档
 * 兼容节点保留。任务于 2026-09-17 promote 时从未有 legacy handler 对照。
 * Locks quest 4338's step axis and necklace-burn branch (QE-054 caliber).
 * The axis follows retail: picking a guide (SETPRO5) explicitly writes 4 -&gt; 5
 * (0x110(0x10f2, 4, 5, ...) in FUN_180fb8390/FUN_180fb84d0), and throwing the necklace into the
 * lava (use-item) enters REWARD with the axis kept at 5 (FUN_180f00820 advances via
 * 0x100(0x10f2, 0, 0) on START/step==5). The faulty batch (caf547879) advanced to 6 and later
 * raised to 7, conflicting with retail; s6 stays only as a legacy-save compatibility node. This
 * quest was promoted on 2026-09-17 without any legacy handler to compare against.
 */
class Quest4338ClientDialogAlignmentTest {
	private static final int GUNDALPUN_ID = 790020;
	private static final int GUIDE_ID = 204394;
	private static final int CURSED_NECKLACE_ID = 182215326;

	@Test
	void keepsTheRetailStepAxisAcrossNecklaceThrowAndReward() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5));

		// 选向导：4 -> 5（真端 0x110(0x10f2, 4, 5, ...) 的显式轴推进）。
		// Picking a guide: 4 -> 5 (retail 0x110(0x10f2, 4, 5, ...) explicit axis write).
		QuestTransition pickGuide = transition(definition, "s4", "s5",
			new QuestEvent.TalkToNpc(GUIDE_ID, QuestDialogAction.SETPRO5.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), pickGuide.actions());

		// 扔项链：直接进 REWARD，步号保持 5（真端 0x100 推进同型）。
		// Necklace throw: straight into REWARD keeping step 5 (retail 0x100 advance).
		QuestTransition throwNecklace = transition(definition, "s5", "reward",
			new QuestEvent.UseItem(CURSED_NECKLACE_ID));
		assertEquals(List.of(new QuestAction.RemoveItem(CURSED_NECKLACE_ID, 1)),
			throwNecklace.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), throwNecklace.afterCommit());

		// 领奖面：REWARD 态与甘达尔彭对话发奖励窗。
		// Reward face: the Gundalpun dialog in REWARD opens the reward window.
		QuestTransition rewardTalk = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(GUNDALPUN_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardTalk.afterCommit());

		// 无 source 自愈边（两条）：错误轴（caf547879 批次）写坏的存档归一为权威值——
		// REWARD/var0=7 -> 5（领奖态）；START/var0=6 -> 5（扔过项链的中间态，回到 s5）。
		// Source-less healing edges (two): saves persisted under the wrong axis (the caf547879
		// batch) normalize to the authoritative value — REWARD/var0=7 -> 5 (reward state);
		// START/var0=6 -> 5 (mid-state after the throw, back to s5).
		QuestTransition rewardHeal = healingEdge(definition, "reward", QuestStatus.REWARD, 7);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), rewardHeal.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), rewardHeal.afterCommit());
		QuestTransition startHeal = healingEdge(definition, "s5", QuestStatus.START, 6);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), startHeal.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.PACKET_ONLY)), startHeal.afterCommit());
	}

	private static QuestTransition healingEdge(QuestDefinition definition, String target,
		QuestStatus status, int staleValue) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(candidate -> candidate.conditions().equals(List.of(
				new QuestCondition.StatusIs(status),
				new QuestCondition.QuestVariableIs("var0", staleValue))))
			.toList();
		assertEquals(1, matches.size(),
			"quest 4338 healing edge for " + status + "/var0=" + staleValue);
		return matches.getFirst();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 4338 " + source + " -> " + target + " " + event);
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
		try (InputStream input = Quest4338ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/4338.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 4338.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
