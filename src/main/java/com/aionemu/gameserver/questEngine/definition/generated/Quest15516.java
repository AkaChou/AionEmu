package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15516 {
	private Quest15516() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15516)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Infiltration/Group] Scout Kalidag Canyon", 1802097, 70, 2147483647, Set.of("ELYOS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 465480L), new QuestReward("EXP", 0, 98623008L), new QuestReward("ITEM", 169405428, 5L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 15550, 0)), Map.of());
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
		builder.on(new QuestEvent.TalkToNpc(806094, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1009, 0)).from("started").when(new QuestCondition.VariableAtLeast("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 98623008L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 169405428, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(238076, 238077, 238078, 238079, 238072, 238073, 238074, 238075, 238068, 238069, 238070, 238071, 238064, 238065, 238066, 238067, 238060, 238061, 238062, 238063, 238056, 238057, 238058, 238059, 238052, 238053, 238054, 238055, 238048, 238049, 238050, 238051, 238044, 238045, 238046, 238047, 238040, 238041, 238042, 238043, 238036, 238037, 238038, 238039, 238032, 238033, 238034, 238035, 238028, 238029, 238030, 238031, 238024, 238025, 238026, 238027, 238095, 238094, 238093, 238092, 238091, 238090, 238089, 238088, 238087, 238086, 238085, 238084, 238083, 238082, 238081, 238080, 238020, 238021, 238022, 238023, 238016, 238017, 238018, 238019, 238012, 238013, 238014, 238015, 238008, 238009, 238010, 238011, 243636, 238004, 243637, 238005, 243638, 238006, 238007, 243632, 238000, 243633, 238001, 243634, 238002, 243635, 238003, 243628, 237996, 243629, 237997, 243630, 241573, 241572, 241571, 241570, 241569, 241568, 241567, 241566, 241565, 241564, 241563, 241562, 241561, 241560, 241559, 237998, 243631, 237999, 243624, 237992, 243625, 237993, 243626, 237994, 243627, 237995, 237988, 243620, 237989, 243621, 237990, 243622, 243623, 237991, 243616, 237984, 243617, 237985, 243618, 237986, 237987, 243619, 243612, 237980, 243613, 237981, 243614, 237982, 243615, 237983, 243608, 237976, 243609, 237977, 243610, 237978, 243611, 237979, 243604, 237972, 243605, 237973, 243606, 237974, 243607, 237975, 237968, 243600, 237969, 243601, 237970, 243602, 237971, 243603, 237964, 237965, 237967, 237966, 237963, 240788, 240787, 237962, 237961, 237960, 237959, 237958, 237957, 237956, 237955, 237954, 237953, 237952, 243599, 243598, 243597, 243596, 243595, 243594, 243593, 243592, 243591, 243590, 243589, 243588, 243587, 243586, 243585, 243584, 243583, 243582, 243581, 243580, 243579, 243578, 243577, 243576, 243575, 243574, 243573, 243572, 243571, 243570, 243569, 243568, 243567, 243566, 243565, 243564, 243563, 243562, 243561, 243560, 243559, 242083, 242082, 242081, 242080, 242079, 242078, 242077, 242076, 242075, 242074, 242073, 242072, 242071, 242070, 242069, 242068, 242067, 242066, 242065, 242064, 242063, 242062, 242061, 242060, 242059, 242058, 242057, 242056, 242055, 242054, 242053, 242052, 242051, 242050, 242049, 242048, 242047, 242046, 242045, 242044, 242043, 242042, 242041, 242040, 242039, 242038, 242037, 242036, 242035, 242034, 242033, 242032, 242031, 242030, 242029, 242028, 242027, 242026, 242025, 242024, 242023, 242022, 242021, 242020, 242019, 242018, 242017, 242016, 242015, 242014, 242013, 242012, 242011, 242010, 242009, 242008, 242007, 242006, 242005, 242004, 240426, 240425, 240424, 240423, 240422, 240421, 240420, 240419, 237488, 237487, 237486, 237485, 237484, 237483, 237482, 237481, 237480, 237479, 237478, 237477, 237476, 237475, 237474, 237473, 237472, 237471, 237470, 237469, 237468, 237467, 237466, 237465))).from("started").priority(1).when(new QuestCondition.VariableBelow("var1", 29)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(238076, 238077, 238078, 238079, 238072, 238073, 238074, 238075, 238068, 238069, 238070, 238071, 238064, 238065, 238066, 238067, 238060, 238061, 238062, 238063, 238056, 238057, 238058, 238059, 238052, 238053, 238054, 238055, 238048, 238049, 238050, 238051, 238044, 238045, 238046, 238047, 238040, 238041, 238042, 238043, 238036, 238037, 238038, 238039, 238032, 238033, 238034, 238035, 238028, 238029, 238030, 238031, 238024, 238025, 238026, 238027, 238095, 238094, 238093, 238092, 238091, 238090, 238089, 238088, 238087, 238086, 238085, 238084, 238083, 238082, 238081, 238080, 238020, 238021, 238022, 238023, 238016, 238017, 238018, 238019, 238012, 238013, 238014, 238015, 238008, 238009, 238010, 238011, 243636, 238004, 243637, 238005, 243638, 238006, 238007, 243632, 238000, 243633, 238001, 243634, 238002, 243635, 238003, 243628, 237996, 243629, 237997, 243630, 241573, 241572, 241571, 241570, 241569, 241568, 241567, 241566, 241565, 241564, 241563, 241562, 241561, 241560, 241559, 237998, 243631, 237999, 243624, 237992, 243625, 237993, 243626, 237994, 243627, 237995, 237988, 243620, 237989, 243621, 237990, 243622, 243623, 237991, 243616, 237984, 243617, 237985, 243618, 237986, 237987, 243619, 243612, 237980, 243613, 237981, 243614, 237982, 243615, 237983, 243608, 237976, 243609, 237977, 243610, 237978, 243611, 237979, 243604, 237972, 243605, 237973, 243606, 237974, 243607, 237975, 237968, 243600, 237969, 243601, 237970, 243602, 237971, 243603, 237964, 237965, 237967, 237966, 237963, 240788, 240787, 237962, 237961, 237960, 237959, 237958, 237957, 237956, 237955, 237954, 237953, 237952, 243599, 243598, 243597, 243596, 243595, 243594, 243593, 243592, 243591, 243590, 243589, 243588, 243587, 243586, 243585, 243584, 243583, 243582, 243581, 243580, 243579, 243578, 243577, 243576, 243575, 243574, 243573, 243572, 243571, 243570, 243569, 243568, 243567, 243566, 243565, 243564, 243563, 243562, 243561, 243560, 243559, 242083, 242082, 242081, 242080, 242079, 242078, 242077, 242076, 242075, 242074, 242073, 242072, 242071, 242070, 242069, 242068, 242067, 242066, 242065, 242064, 242063, 242062, 242061, 242060, 242059, 242058, 242057, 242056, 242055, 242054, 242053, 242052, 242051, 242050, 242049, 242048, 242047, 242046, 242045, 242044, 242043, 242042, 242041, 242040, 242039, 242038, 242037, 242036, 242035, 242034, 242033, 242032, 242031, 242030, 242029, 242028, 242027, 242026, 242025, 242024, 242023, 242022, 242021, 242020, 242019, 242018, 242017, 242016, 242015, 242014, 242013, 242012, 242011, 242010, 242009, 242008, 242007, 242006, 242005, 242004, 240426, 240425, 240424, 240423, 240422, 240421, 240420, 240419, 237488, 237487, 237486, 237485, 237484, 237483, 237482, 237481, 237480, 237479, 237478, 237477, 237476, 237475, 237474, 237473, 237472, 237471, 237470, 237469, 237468, 237467, 237466, 237465))).from("started").priority(0).when(new QuestCondition.VariableAtLeast("var1", 29)).then(new QuestAction.SetVariable("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
