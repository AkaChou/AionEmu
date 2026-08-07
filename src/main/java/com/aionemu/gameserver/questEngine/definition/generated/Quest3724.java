package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest3724 {
	private Quest3724() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(3724)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Group] Dredgion Blues", 1104721, 51, 55, Set.of("ELYOS"), "QUEST", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182202196, 6), new QuestItemRequirement(182202197, 6), new QuestItemRequirement(182202198, 1), new QuestItemRequirement(182202199, 1)), List.of(new QuestReward("EXP", 0, 4084089L), new QuestReward("ITEM", 186000469, 18L)), List.of(new QuestDrop(216876, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216883, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216878, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216880, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216889, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216890, 182202196, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216882, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216877, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216885, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216879, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216881, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216884, 182202197, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216887, 182202198, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(216888, 182202199, 100, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 1, 0, 1, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 0))));
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
		addTransition29(builder);
		addTransition30(builder);
		addTransition31(builder);
		addTransition32(builder);
		addTransition33(builder);
		addTransition34(builder);
		addTransition35(builder);
		addTransition36(builder);
		addTransition37(builder);
		addTransition38(builder);
		addTransition39(builder);
		addTransition40(builder);
		addTransition41(builder);
		addTransition42(builder);
		addTransition43(builder);
		addTransition44(builder);
		addTransition45(builder);
		addTransition46(builder);
		addTransition47(builder);
		addTransition48(builder);
		addTransition49(builder);
		addTransition50(builder);
		addTransition51(builder);
		addTransition52(builder);
		addTransition53(builder);
		addTransition54(builder);
		addTransition55(builder);
		addTransition56(builder);
		addTransition57(builder);
		addTransition58(builder);
		addTransition59(builder);
		addTransition60(builder);
		addTransition61(builder);
		addTransition62(builder);
		addTransition63(builder);
		addTransition64(builder);
		addTransition65(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 10000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 39, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182202196, 6, true)).when(new QuestCondition.HasItem(182202197, 6, true)).when(new QuestCondition.HasItem(182202198, 1, true)).when(new QuestCondition.HasItem(182202199, 1, true)).then(new QuestAction.RemoveItem(182202196, 6)).then(new QuestAction.RemoveItem(182202197, 6)).then(new QuestAction.RemoveItem(182202198, 1)).then(new QuestAction.RemoveItem(182202199, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 39, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 20002, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182202196, 6, true)).when(new QuestCondition.HasItem(182202197, 6, true)).when(new QuestCondition.HasItem(182202198, 1, true)).when(new QuestCondition.HasItem(182202199, 1, true)).then(new QuestAction.RemoveItem(182202196, 6)).then(new QuestAction.RemoveItem(182202197, 6)).then(new QuestAction.RemoveItem(182202198, 1)).then(new QuestAction.RemoveItem(182202199, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 20002, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 10255, 0)).from("started").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799069, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 10000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 39, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182202196, 6, true)).when(new QuestCondition.HasItem(182202197, 6, true)).when(new QuestCondition.HasItem(182202198, 1, true)).when(new QuestCondition.HasItem(182202199, 1, true)).then(new QuestAction.RemoveItem(182202196, 6)).then(new QuestAction.RemoveItem(182202197, 6)).then(new QuestAction.RemoveItem(182202198, 1)).then(new QuestAction.RemoveItem(182202199, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 39, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 20002, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182202196, 6, true)).when(new QuestCondition.HasItem(182202197, 6, true)).when(new QuestCondition.HasItem(182202198, 1, true)).when(new QuestCondition.HasItem(182202199, 1, true)).then(new QuestAction.RemoveItem(182202196, 6)).then(new QuestAction.RemoveItem(182202197, 6)).then(new QuestAction.RemoveItem(182202198, 1)).then(new QuestAction.RemoveItem(182202199, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 20002, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 10255, 0)).from("started").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798928, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4084089L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 18L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
