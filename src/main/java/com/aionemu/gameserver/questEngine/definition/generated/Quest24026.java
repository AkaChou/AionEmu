package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest24026 {
	private Quest24026() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(24026)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("A Hand from Each Side", 1129899, 35, 2147483647, Set.of("ASMODIANS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(24025, 24024, 24023, 24022, 24021, 24020), List.of(), List.of(new QuestReward("EXP", 0, 3504765L), new QuestReward("TITLE", 60, 1L), new QuestReward("SELECTABLE_ITEM", 114101704, 1L), new QuestReward("SELECTABLE_ITEM", 114301833, 1L), new QuestReward("SELECTABLE_ITEM", 114301835, 1L), new QuestReward("SELECTABLE_ITEM", 114501742, 1L), new QuestReward("SELECTABLE_ITEM", 114501744, 1L), new QuestReward("SELECTABLE_ITEM", 114601583, 1L), new QuestReward("ITEM", 186000008, 20L), new QuestReward("ITEM", 190200000, 10L)), List.of(), Set.of("GLADIATOR", "SONGWEAVER", "RANGER", "SPIRIT_MASTER", "CHANTER", "TECHNIST", "TEMPLAR", "GUNSLINGER", "AETHERTECH", "SORCERER", "ASSASSIN", "CLERIC"), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("unfinished", 2041, 0), new QuestStartCondition("noacquired", 2041, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 3, 0, 4, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("defense", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("defense-done", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 4))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).when(new QuestCondition.QuestsFinished(Set.of(24025, 24024, 24023, 24022, 24021, 24020))).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).when(new QuestCondition.QuestsFinished(Set.of(24025, 24024, 24023, 24022, 24021, 24020))).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 10000, 0)).from("started").then(new QuestAction.GiveItem(182215371, 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 220020000, 2795.9f, 478.37f, 265.86f, (byte) 51));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204403, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204403, 10001, 0)).from("s1").then(new QuestAction.GiveItem(182215372, 1)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 220020000, 3025.54f, 868.31f, 363.22f, (byte) 14));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204432, 31, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204432, 10002, 0)).from("s2").goTo("defense");
		builder.afterCommit(new AfterCommitAction.StartQuestTimer(120, new QuestTimerPolicy(new QuestTimerPolicy.Identity("24026-defense", QuestTimerPolicy.Scope.PLAYER_QUEST), QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE, QuestTimerPolicy.Delivery.AT_MOST_ONCE)));
		builder.afterCommit(new AfterCommitAction.SpawnNpcRandom("defense-mob", List.of(new QuestSpawnVariant(213576, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 254.74f, 236.72f, 217.48f, (byte) 95)), new QuestSpawnVariant(213577, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 257.92f, 237.39f, 217.48f, (byte) 95)), new QuestSpawnVariant(213578, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 261.86f, 237.5f, 217.48f, (byte) 95))), true));
		builder.afterCommit(new AfterCommitAction.AttackNpcTemplate("defense-mob", 204432));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(213577, 213576, 213579, 213578))).from("defense").goTo("defense");
		builder.afterCommit(new AfterCommitAction.SpawnNpcRandom("defense-mob", List.of(new QuestSpawnVariant(213576, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 254.74f, 236.72f, 217.48f, (byte) 95)), new QuestSpawnVariant(213577, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 257.92f, 237.39f, 217.48f, (byte) 95)), new QuestSpawnVariant(213578, new QuestSpawnLocation.Fixed(320040000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 261.86f, 237.5f, 217.48f, (byte) 95))), true));
		builder.afterCommit(new AfterCommitAction.AttackNpcTemplate("defense-mob", 204432));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.QuestTimerEnd()).from("defense").goTo("defense-done");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("defense").goTo("s2");
		builder.afterCommit(new AfterCommitAction.CancelQuestTimer(new QuestTimerPolicy.Identity("24026-defense", QuestTimerPolicy.Scope.PLAYER_QUEST)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("defense").goTo("s2");
		builder.afterCommit(new AfterCommitAction.CancelQuestTimer(new QuestTimerPolicy.Identity("24026-defense", QuestTimerPolicy.Scope.PLAYER_QUEST)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204432, 31, 0)).from("defense-done").goTo("defense-done");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204432, 10003, 0)).from("defense-done").then(new QuestAction.RemoveItem(182215371, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 220020000, 248.52255f, 2398.9722f, 452.81012f, (byte) 48));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101704, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301833, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301835, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501742, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501744, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(6)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204301, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 60, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601583, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000008, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 190200000, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(7)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
