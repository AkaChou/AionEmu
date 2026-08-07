package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest1929 {
	private Quest1929() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(1929)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("A Sliver Of Darkness", 1102929, 20, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 25000L), new QuestReward("EXP", 0, 457760L), new QuestReward("ITEM", 162000048, 10L)), List.of(), Set.of(), "", 0, 1, 1, true, true, false, 1, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.ofEntries(Map.entry("FIGHTER", List.of(new QuestReward("ITEM", 140001110, 1L))), Map.entry("KNIGHT", List.of(new QuestReward("ITEM", 140001133, 1L))), Map.entry("RANGER", List.of(new QuestReward("ITEM", 140001159, 1L))), Map.entry("ASSASSIN", List.of(new QuestReward("ITEM", 140001146, 1L))), Map.entry("WIZARD", List.of(new QuestReward("ITEM", 140001180, 1L))), Map.entry("ELEMENTALIST", List.of(new QuestReward("ITEM", 140001204, 1L))), Map.entry("PRIEST", List.of(new QuestReward("ITEM", 140001237, 1L))), Map.entry("CHANTER", List.of(new QuestReward("ITEM", 140001218, 1L))), Map.entry("GUNSLINGER", List.of(new QuestReward("ITEM", 140001257, 1L))), Map.entry("SONGWEAVER", List.of(new QuestReward("ITEM", 140001288, 1L))), Map.entry("AETHERTECH", List.of(new QuestReward("ITEM", 140001272, 1L)))));
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("step", 0, 7, 0, 98, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("step", 0))));
		builder.node("started0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 0))));
		builder.node("started1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 1))));
		builder.node("started2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 2))));
		builder.node("instance93", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 93))));
		builder.node("flight94", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 94))));
		builder.node("spawned98", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 98))));
		builder.node("equipped96", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 96))));
		builder.node("fight97", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 97))));
		builder.node("postFight8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 8))));
		builder.node("started9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 9))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("step", 9))));
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
		addTransition66(builder);
		addTransition67(builder);
		addTransition68(builder);
		addTransition69(builder);
		addTransition70(builder);
		addTransition71(builder);
		addTransition72(builder);
		addTransition73(builder);
		addTransition74(builder);
		addTransition75(builder);
		addTransition76(builder);
		addTransition77(builder);
		addTransition78(builder);
		addTransition79(builder);
		addTransition80(builder);
		addTransition81(builder);
		addTransition82(builder);
		addTransition83(builder);
		addTransition84(builder);
		addTransition85(builder);
		addTransition86(builder);
		addTransition87(builder);
		addTransition88(builder);
		addTransition89(builder);
		addTransition90(builder);
		addTransition91(builder);
		addTransition92(builder);
		addTransition93(builder);
		addTransition94(builder);
		addTransition95(builder);
		addTransition96(builder);
		addTransition97(builder);
		addTransition98(builder);
		addTransition99(builder);
		addTransition100(builder);
		addTransition101(builder);
		addTransition102(builder);
		addTransition103(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).when(new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST, true)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).when(new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST, false)).goTo("started0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST, true)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 31, 0)).from("started0").goTo("started0");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10000, 0)).from("started0").then(new QuestAction.SetVariable("step", 1)).goTo("started1");
		builder.afterCommit(new AfterCommitAction.PlayMovie(102));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203852, 31, 0)).from("started1").goTo("started1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203852, 10001, 0)).from("started1").then(new QuestAction.SetVariable("step", 2)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 210030000, 2315.0986f, 1798.2798f, 195.26416f, (byte) 25));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203164, 31, 0)).from("started2").goTo("started2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203164, 10002, 0)).from("started2").then(new QuestAction.SetVariable("step", 93)).goTo("instance93");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(new QuestInstanceTarget.NextAvailable(310070000), 310070000, 338.0f, 101.0f, 1191.0f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205110, 31, 0)).from("instance93").goTo("instance93");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205110, 10003, 0)).from("instance93").then(new QuestAction.SetVariable("step", 94)).goTo("flight94");
		builder.afterCommit(new AfterCommitAction.FlightTeleport(31001));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700240, -1, 0)).from("flight94").goTo("flight94");
		builder.afterCommit(new AfterCommitAction.PlayMovie(155));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.MovieEnd(155)).from("flight94").then(new QuestAction.SetVariable("step", 98)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("1929-ecus", 205111, new QuestSpawnLocation.Fixed(310070000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 197.6f, 265.9f, 1374.0f, (byte) 0)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 31, 0)).from("spawned98").goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GiveItem(140000003, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GiveItem(140000003, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GiveItem(140000003, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GiveItem(140000003, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GiveItem(140000002, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GiveItem(140000002, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GiveItem(140000002, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GiveItem(140000003, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GiveItem(140000004, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GiveItem(140000004, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2546, 0)).from("spawned98").when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GiveItem(140000004, 1)).goTo("spawned98");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EquipItem(140000001)).from("spawned98").then(new QuestAction.SetVariable("step", 96)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EquipItem(140000002)).from("spawned98").then(new QuestAction.SetVariable("step", 96)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EquipItem(140000003)).from("spawned98").then(new QuestAction.SetVariable("step", 96)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EquipItem(140000004)).from("spawned98").then(new QuestAction.SetVariable("step", 96)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, true)).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000002, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000003, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, -1, 0)).from("equipped96").when(new QuestCondition.EquippedItem(140000004, 1, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).goTo("equipped96");
		builder.afterCommit(new AfterCommitAction.ShowDialogWindow(1));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205111, 2720, 0)).from("equipped96").then(new QuestAction.SetVariable("step", 97)).goTo("fight97");
		builder.afterCommit(new AfterCommitAction.DeleteInteractionNpc(false));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("1929-boss", 212992, new QuestSpawnLocation.Fixed(310070000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 195.3323f, 265.31827f, 1374.1426f, (byte) 8)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(212992)).from("fight97").then(new QuestAction.SetVariable("step", 8)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 210030000, 2315.9f, 1800.0f, 195.2f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).when(new QuestCondition.StatusIs(QuestStatus.START)).when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.VariableAtLeast("step", 93)).when(new QuestCondition.VariableBelow("step", 99)).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.UnequipItem(140000002, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.UnequipItem(140000003, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("postFight8").when(new QuestCondition.WorldIs(310070000, false)).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.UnequipItem(140000004, 1)).goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203164, 31, 0)).from("postFight8").goTo("postFight8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3057));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203164, 10006, 0)).from("postFight8").then(new QuestAction.SetVariable("step", 9)).goTo("started9");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 110010000, 1878.27f, 1513.74f, 812.675f, (byte) 25));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203701, 31, 0)).from("started9").goTo("started9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3398));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203701, 10007, 0)).from("started9").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001110, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001133, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001159, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001146, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001180, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001204, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001237, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001218, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001257, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001288, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("GOLD", 0, 25000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 457760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 140001272, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
