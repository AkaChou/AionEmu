package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15524 {
	private Quest15524() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15524)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Daily] Protect the Ancient Temple of Life", 1802105, 72, 2147483647, Set.of("ELYOS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 94025232L), new QuestReward("ITEM", 188054912, 1L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 15550, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 6, 6, 0, 30, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var1", 30), Map.entry("var0", 0))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1009, 0)).from("started").when(new QuestCondition.VariableAtLeast("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806097, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 94025232L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(239861, 239860, 239859, 239858, 239857, 239856, 239855, 239854, 239853, 239852, 239851, 239850, 239849, 239848, 239847, 239846, 239845, 239844, 239843, 239842, 239841, 239840, 239839, 239838, 239837, 239836, 239835, 239834, 239833, 239832, 239831, 239830, 239829, 239828, 239827, 239826, 239825, 239824, 239823, 239822, 239821, 239820, 239819, 239818, 239817, 239816, 239815, 239814, 239813, 239812, 239811, 239810, 239809, 239808, 239807, 239806, 239805, 239804, 239803, 239802, 239801, 239068, 239067, 239066, 239065, 239064, 239063, 239062, 239061, 239060, 239059, 239058, 239057, 239056, 239055, 239054, 239053, 239052, 239051, 239050, 239049, 239048, 239047, 239046, 239045, 239044, 239043, 239042, 239041, 243003, 243002, 243001, 243000, 242999, 242998, 242997, 242996, 242995, 242994, 242993, 242992, 242991, 242990, 242989, 242988, 242987, 242986, 242985, 242984, 242983, 242982, 242981, 242980, 242979, 242978, 242977, 242976, 242975, 242974, 242973, 242972, 242971, 242970, 242969, 242968, 242967, 242966, 242965, 242964, 242963, 242962, 242961, 242960, 242959, 242958, 242957, 242956, 242955, 242954, 242953, 242952, 242951, 242950, 242949, 242948, 242947, 242946, 242945, 242944, 242943, 242942, 242941, 242940, 242939, 242938, 242937, 242936, 242935, 242934, 242933, 242932, 242931, 242930, 242929, 242928, 242927, 242926, 242925, 242924, 241772, 241771, 241770, 241769, 241768, 241767, 240552, 240551, 240550, 240549, 239968, 239967, 239966, 239965, 239964, 239963, 239962, 239961, 239960, 239959, 239958, 239957, 239956, 239955, 239954, 239953, 239952, 239951, 239950, 239949, 239948, 239947, 239946, 239945, 239944, 239943, 239942, 239941, 239940, 239939, 239938, 239937, 239936, 239935, 239934, 239933, 239932, 239931, 239930, 239929, 239928, 239927, 239926, 239925, 239924, 239923, 239922, 239921, 239920, 239919, 239918, 239917, 239916, 239915, 239914, 239913, 239912, 239911, 239910, 239909, 239908, 239907, 239906, 239905, 239904, 239903, 239902, 239901, 239900, 239899, 239898, 239897, 239896, 239895, 239894, 239893, 239892, 239891, 239890, 239889, 239888, 239887, 239886, 239885, 239884, 239883, 239882, 239881, 239880, 239879, 239878, 239877, 239876, 239875, 239874, 239873, 239872, 239871, 239870, 239869, 239868, 239867, 239866, 239865, 239864, 239863, 239862))).from("started").priority(1).when(new QuestCondition.VariableBelow("var1", 29)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(239861, 239860, 239859, 239858, 239857, 239856, 239855, 239854, 239853, 239852, 239851, 239850, 239849, 239848, 239847, 239846, 239845, 239844, 239843, 239842, 239841, 239840, 239839, 239838, 239837, 239836, 239835, 239834, 239833, 239832, 239831, 239830, 239829, 239828, 239827, 239826, 239825, 239824, 239823, 239822, 239821, 239820, 239819, 239818, 239817, 239816, 239815, 239814, 239813, 239812, 239811, 239810, 239809, 239808, 239807, 239806, 239805, 239804, 239803, 239802, 239801, 239068, 239067, 239066, 239065, 239064, 239063, 239062, 239061, 239060, 239059, 239058, 239057, 239056, 239055, 239054, 239053, 239052, 239051, 239050, 239049, 239048, 239047, 239046, 239045, 239044, 239043, 239042, 239041, 243003, 243002, 243001, 243000, 242999, 242998, 242997, 242996, 242995, 242994, 242993, 242992, 242991, 242990, 242989, 242988, 242987, 242986, 242985, 242984, 242983, 242982, 242981, 242980, 242979, 242978, 242977, 242976, 242975, 242974, 242973, 242972, 242971, 242970, 242969, 242968, 242967, 242966, 242965, 242964, 242963, 242962, 242961, 242960, 242959, 242958, 242957, 242956, 242955, 242954, 242953, 242952, 242951, 242950, 242949, 242948, 242947, 242946, 242945, 242944, 242943, 242942, 242941, 242940, 242939, 242938, 242937, 242936, 242935, 242934, 242933, 242932, 242931, 242930, 242929, 242928, 242927, 242926, 242925, 242924, 241772, 241771, 241770, 241769, 241768, 241767, 240552, 240551, 240550, 240549, 239968, 239967, 239966, 239965, 239964, 239963, 239962, 239961, 239960, 239959, 239958, 239957, 239956, 239955, 239954, 239953, 239952, 239951, 239950, 239949, 239948, 239947, 239946, 239945, 239944, 239943, 239942, 239941, 239940, 239939, 239938, 239937, 239936, 239935, 239934, 239933, 239932, 239931, 239930, 239929, 239928, 239927, 239926, 239925, 239924, 239923, 239922, 239921, 239920, 239919, 239918, 239917, 239916, 239915, 239914, 239913, 239912, 239911, 239910, 239909, 239908, 239907, 239906, 239905, 239904, 239903, 239902, 239901, 239900, 239899, 239898, 239897, 239896, 239895, 239894, 239893, 239892, 239891, 239890, 239889, 239888, 239887, 239886, 239885, 239884, 239883, 239882, 239881, 239880, 239879, 239878, 239877, 239876, 239875, 239874, 239873, 239872, 239871, 239870, 239869, 239868, 239867, 239866, 239865, 239864, 239863, 239862))).from("started").priority(0).when(new QuestCondition.VariableAtLeast("var1", 29)).then(new QuestAction.SetVariable("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
