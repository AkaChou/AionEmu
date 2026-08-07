package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15002 {
	private Quest15002() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15002)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Cold Hands, Warm Luciferin", 1800714, 55, 2147483647, Set.of("ELYOS"), "IMPORTANT", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182215663, 7)), List.of(new QuestReward("GOLD", 0, 96300L), new QuestReward("EXP", 0, 8828737L), new QuestReward("ITEM", 186000231, 10L), new QuestReward("ITEM", 186000237, 15L), new QuestReward("SELECTABLE_ITEM", 160002336, 7L), new QuestReward("SELECTABLE_ITEM", 160002337, 7L), new QuestReward("SELECTABLE_ITEM", 160002338, 7L), new QuestReward("SELECTABLE_ITEM", 160002339, 7L), new QuestReward("SELECTABLE_ITEM", 160002340, 7L), new QuestReward("SELECTABLE_ITEM", 160002341, 7L)), List.of(new QuestDrop(235792, 182215663, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(235793, 182215663, 100, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 0))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1009, 0)).from("started").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002336, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002337, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002338, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002339, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002340, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804698, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 96300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8828737L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 15L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 160002341, 7L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
