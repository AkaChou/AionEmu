package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest24051 {
	private Quest24051() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(24051)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Investigate the Disappearance", 1129924, 36, 2147483647, Set.of("ASMODIANS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182215377, 1)), List.of(new QuestReward("EXP", 0, 4272047L), new QuestReward("ITEM", 162000050, 40L), new QuestReward("ITEM", 186000009, 20L), new QuestReward("ITEM", 188053200, 1L), new QuestReward("SELECTABLE_ITEM", 100001745, 1L), new QuestReward("SELECTABLE_ITEM", 100101342, 1L), new QuestReward("SELECTABLE_ITEM", 100201511, 1L), new QuestReward("SELECTABLE_ITEM", 100501322, 1L), new QuestReward("SELECTABLE_ITEM", 100601439, 1L), new QuestReward("SELECTABLE_ITEM", 100901383, 1L), new QuestReward("SELECTABLE_ITEM", 101301275, 1L), new QuestReward("SELECTABLE_ITEM", 101501365, 1L), new QuestReward("SELECTABLE_ITEM", 101701376, 1L), new QuestReward("SELECTABLE_ITEM", 101801226, 1L), new QuestReward("SELECTABLE_ITEM", 101901135, 1L), new QuestReward("SELECTABLE_ITEM", 102001254, 1L), new QuestReward("SELECTABLE_ITEM", 102101080, 1L)), List.of(new QuestDrop(213281, 182215377, 100, true, 5, QuestDropScope.GROUP), new QuestDrop(213282, 182215377, 100, true, 5, QuestDropScope.GROUP), new QuestDrop(213266, 182215377, 80, true, 5, QuestDropScope.GROUP), new QuestDrop(213267, 182215377, 80, true, 5, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("unfinished", 2061, 0), new QuestStartCondition("noacquired", 2061, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 5, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("s3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("s4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("s5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.of()));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
	}

	private static void addTransitionBatch0(QuestDsl.QuestBuilder builder) {
		addTransition0(builder);
		addTransition1(builder);
		addTransition2(builder);
		addTransition3(builder);
		addTransition4(builder);
		addTransition5(builder);
		addTransition6(builder);
		addTransition7(builder);
		addTransition8(builder);
		addTransition9(builder);
		addTransition10(builder);
		addTransition11(builder);
		addTransition12(builder);
		addTransition13(builder);
		addTransition14(builder);
		addTransition15(builder);
		addTransition16(builder);
		addTransition17(builder);
		addTransition18(builder);
		addTransition19(builder);
		addTransition20(builder);
		addTransition21(builder);
		addTransition22(builder);
		addTransition23(builder);
		addTransition24(builder);
		addTransition25(builder);
		addTransition26(builder);
		addTransition27(builder);
		addTransition28(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 10000, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204749, 31, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204749, 10001, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.GiveItem(182215375, 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182215375, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.RemoveItem(182215375, 1)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 31, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 10003, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204800, 31, 0)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204800, 10004, 0)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.GiveItem(182215376, 1)).then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700359, -1, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).when(new QuestCondition.HasItem(182215377, 1, true)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 220040000, 1757.82f, 1392.94f, 401.75f, (byte) 94));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("MINE_PORT_220040000")).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.PlayMovie(236));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.MovieEnd(236)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.RemoveItem(182215377, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, -1, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 1009, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 8, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100001745, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 9, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100101342, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 10, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100201511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 11, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100501322, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 12, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100601439, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 13, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100901383, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 14, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101301275, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 15, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101501365, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 16, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101701376, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 17, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101801226, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 18, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101901135, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 19, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102001254, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204707, 20, 0)).from("reward").then(new QuestAction.RemoveItem(182215376, 1)).then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000009, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053200, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102101080, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
