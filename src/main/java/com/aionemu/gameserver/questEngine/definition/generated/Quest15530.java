package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15530 {
	private Quest15530() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15530)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Group/Daily] Protect the Krall Aether Mine", 1802111, 70, 2147483647, Set.of("ELYOS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 85792608L), new QuestReward("ITEM", 188054912, 1L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 15550, 0)), Map.of());
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
		builder.on(new QuestEvent.TalkToNpc(806099, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1009, 0)).from("started").when(new QuestCondition.VariableAtLeast("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806099, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(243203, 243202, 243201, 243200, 243199, 243198, 243197, 243196, 243195, 243194, 243193, 243192, 243191, 243190, 243189, 243188, 243187, 243186, 243185, 243184, 243183, 243182, 243181, 243180, 243179, 243178, 243177, 243176, 243175, 243174, 243173, 243172, 243171, 243170, 243169, 243168, 243167, 243166, 243165, 243164, 243163, 243162, 243161, 243160, 243159, 243158, 243157, 243156, 243155, 243154, 243153, 243152, 243151, 243150, 243149, 243148, 243147, 243146, 243145, 243144, 243143, 243142, 243141, 243140, 243139, 243138, 243137, 243136, 243135, 243134, 243133, 243132, 243131, 243130, 243129, 243128, 243127, 243126, 243125, 243124, 243123, 243122, 243121, 243120, 243119, 243118, 243117, 243116, 243115, 243114, 243113, 243112, 243111, 243110, 243109, 243108, 243107, 243106, 243105, 243104, 243103, 243102, 243101, 243100, 243099, 243098, 243097, 243096, 243095, 243094, 243093, 243092, 243091, 243090, 243089, 243088, 243087, 243086, 243085, 243084, 243083, 243082, 243081, 243080, 243079, 243078, 243077, 243076, 243075, 243074, 243073, 243072, 243071, 243070, 243069, 243068, 243067, 243066, 243065, 243064, 240274, 240273, 240272, 240271, 240270, 240269, 240268, 240267, 240266, 240265, 240264, 240263, 240262, 240261, 240260, 240259, 240258, 240257, 240256, 240255, 240254, 240253, 240252, 240251, 240250, 240249, 240248, 240247, 240246, 240245, 240244, 240243, 240242, 240241, 240240, 240239, 240238, 240237, 240236, 240235, 240234, 240233, 240232, 240231, 240230, 240229, 240228, 240227, 240226, 240225, 240224, 240223, 240222, 240221, 240220, 240219, 240218, 240217, 240216, 240215, 240214, 240213, 240212, 240211, 240210, 240209, 240208, 240207, 240206, 240205, 240204, 240203, 240202, 240201, 240200, 240199, 240198, 240197, 240196, 240195, 240194, 240193, 240192, 240191, 240190, 240189, 240188, 240187, 240186, 240185, 240184, 240183, 240182, 240181, 240180, 240179, 240178, 240177, 240176, 240175, 240174, 240173, 240172, 240171, 240170, 240169, 240168, 240167, 240166, 240165, 240164, 240163, 240162, 240161, 240160, 240159, 240158, 240157, 240156, 240155, 240154, 240153, 240152, 240151, 240150, 240149, 240148, 240147, 240146, 240145, 240144, 240143, 240142, 240141, 240140, 240139, 240138, 240137, 240136, 240135, 240134, 240133, 240132, 240131, 240094, 240093, 240092, 240091, 240090, 240089, 240088, 240087, 240086, 240085, 240084, 240083, 240082, 240081, 240080, 240079, 240078, 240077, 240076, 240075, 240074, 240073, 240072, 240071, 240070, 240069, 240068, 240067, 240066, 240065, 240064, 240063, 240062, 240061, 240060, 240059, 241802, 241801, 241800, 239116, 239117, 239118, 239119, 241793, 241792, 241791, 239112, 239113, 239114, 239115, 239108, 239109, 239110, 239111, 239104, 239105, 239106, 239107, 239100, 239101, 239102, 239103, 239096, 239097, 239098, 239099, 239088, 239089, 239084, 239085, 239086, 239087, 240880, 240879, 240878, 240877, 240876, 240875, 240874, 240873, 240872, 240871, 240870, 240869, 240868, 240867, 240866, 240865, 240864, 240863, 240862, 240861, 240860, 240859, 240858, 240857, 240856, 240855, 240854, 240853, 240852, 240851, 240850, 240849, 240848, 240847, 240846, 240845, 240844, 240843, 240842, 240841, 240840, 240839, 240838, 240837, 240836, 240835, 240834, 240833, 240832, 240831, 240830, 240829, 240828, 240827, 240826, 240825, 240824, 240823, 240822, 240821, 240820, 240819, 240818, 240817, 240816, 240815, 240814, 240813, 240812, 240811, 240810, 240809, 240808, 240807, 240806, 240805, 240804, 240803, 240802, 240801, 240800, 240799, 240798, 240797, 240782, 240781))).from("started").priority(1).when(new QuestCondition.VariableBelow("var1", 29)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(243203, 243202, 243201, 243200, 243199, 243198, 243197, 243196, 243195, 243194, 243193, 243192, 243191, 243190, 243189, 243188, 243187, 243186, 243185, 243184, 243183, 243182, 243181, 243180, 243179, 243178, 243177, 243176, 243175, 243174, 243173, 243172, 243171, 243170, 243169, 243168, 243167, 243166, 243165, 243164, 243163, 243162, 243161, 243160, 243159, 243158, 243157, 243156, 243155, 243154, 243153, 243152, 243151, 243150, 243149, 243148, 243147, 243146, 243145, 243144, 243143, 243142, 243141, 243140, 243139, 243138, 243137, 243136, 243135, 243134, 243133, 243132, 243131, 243130, 243129, 243128, 243127, 243126, 243125, 243124, 243123, 243122, 243121, 243120, 243119, 243118, 243117, 243116, 243115, 243114, 243113, 243112, 243111, 243110, 243109, 243108, 243107, 243106, 243105, 243104, 243103, 243102, 243101, 243100, 243099, 243098, 243097, 243096, 243095, 243094, 243093, 243092, 243091, 243090, 243089, 243088, 243087, 243086, 243085, 243084, 243083, 243082, 243081, 243080, 243079, 243078, 243077, 243076, 243075, 243074, 243073, 243072, 243071, 243070, 243069, 243068, 243067, 243066, 243065, 243064, 240274, 240273, 240272, 240271, 240270, 240269, 240268, 240267, 240266, 240265, 240264, 240263, 240262, 240261, 240260, 240259, 240258, 240257, 240256, 240255, 240254, 240253, 240252, 240251, 240250, 240249, 240248, 240247, 240246, 240245, 240244, 240243, 240242, 240241, 240240, 240239, 240238, 240237, 240236, 240235, 240234, 240233, 240232, 240231, 240230, 240229, 240228, 240227, 240226, 240225, 240224, 240223, 240222, 240221, 240220, 240219, 240218, 240217, 240216, 240215, 240214, 240213, 240212, 240211, 240210, 240209, 240208, 240207, 240206, 240205, 240204, 240203, 240202, 240201, 240200, 240199, 240198, 240197, 240196, 240195, 240194, 240193, 240192, 240191, 240190, 240189, 240188, 240187, 240186, 240185, 240184, 240183, 240182, 240181, 240180, 240179, 240178, 240177, 240176, 240175, 240174, 240173, 240172, 240171, 240170, 240169, 240168, 240167, 240166, 240165, 240164, 240163, 240162, 240161, 240160, 240159, 240158, 240157, 240156, 240155, 240154, 240153, 240152, 240151, 240150, 240149, 240148, 240147, 240146, 240145, 240144, 240143, 240142, 240141, 240140, 240139, 240138, 240137, 240136, 240135, 240134, 240133, 240132, 240131, 240094, 240093, 240092, 240091, 240090, 240089, 240088, 240087, 240086, 240085, 240084, 240083, 240082, 240081, 240080, 240079, 240078, 240077, 240076, 240075, 240074, 240073, 240072, 240071, 240070, 240069, 240068, 240067, 240066, 240065, 240064, 240063, 240062, 240061, 240060, 240059, 241802, 241801, 241800, 239116, 239117, 239118, 239119, 241793, 241792, 241791, 239112, 239113, 239114, 239115, 239108, 239109, 239110, 239111, 239104, 239105, 239106, 239107, 239100, 239101, 239102, 239103, 239096, 239097, 239098, 239099, 239088, 239089, 239084, 239085, 239086, 239087, 240880, 240879, 240878, 240877, 240876, 240875, 240874, 240873, 240872, 240871, 240870, 240869, 240868, 240867, 240866, 240865, 240864, 240863, 240862, 240861, 240860, 240859, 240858, 240857, 240856, 240855, 240854, 240853, 240852, 240851, 240850, 240849, 240848, 240847, 240846, 240845, 240844, 240843, 240842, 240841, 240840, 240839, 240838, 240837, 240836, 240835, 240834, 240833, 240832, 240831, 240830, 240829, 240828, 240827, 240826, 240825, 240824, 240823, 240822, 240821, 240820, 240819, 240818, 240817, 240816, 240815, 240814, 240813, 240812, 240811, 240810, 240809, 240808, 240807, 240806, 240805, 240804, 240803, 240802, 240801, 240800, 240799, 240798, 240797, 240782, 240781))).from("started").priority(0).when(new QuestCondition.VariableAtLeast("var1", 29)).then(new QuestAction.SetVariable("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
