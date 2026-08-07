package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest10521 {
	private Quest10521() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(10521)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Memories of Eternity", 1802009, 65, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 451980L), new QuestReward("SELECTABLE_ITEM", 100201657, 1L), new QuestReward("SELECTABLE_ITEM", 100001994, 1L), new QuestReward("SELECTABLE_ITEM", 100101476, 1L), new QuestReward("SELECTABLE_ITEM", 100901511, 1L), new QuestReward("SELECTABLE_ITEM", 101301395, 1L), new QuestReward("SELECTABLE_ITEM", 101501497, 1L), new QuestReward("SELECTABLE_ITEM", 101701492, 1L), new QuestReward("SELECTABLE_ITEM", 100601552, 1L), new QuestReward("SELECTABLE_ITEM", 100501434, 1L), new QuestReward("SELECTABLE_ITEM", 102001355, 1L), new QuestReward("SELECTABLE_ITEM", 101801327, 1L), new QuestReward("SELECTABLE_ITEM", 101901232, 1L), new QuestReward("SELECTABLE_ITEM", 102101170, 1L), new QuestReward("ITEM", 187000186, 1L), new QuestReward("ITEM", 166100009, 10L)), List.of(), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 306, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 10520, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 14, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("s3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("s4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("s5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("s6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 6))));
		builder.node("s7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 7))));
		builder.node("s8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 8))));
		builder.node("s9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 9))));
		builder.node("s10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("s11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 11))));
		builder.node("s12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 12))));
		builder.node("s13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 13))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 14))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806134, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806134, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806134, 10000, 0)).from("started").then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1353, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1353));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 10001, 0)).from("s1").then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703164, -1, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.PlayMovie(999));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.MovieEnd(999)).from("s2").then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(new QuestInstanceTarget.NextAvailable(301570000), 301570000, 737.0f, 512.0f, 469.0f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("ID_ETERNITY_Q_SENSORYAREA_A_301570000")).from("s3").then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(857783)).from("s4").then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731667, -1, 0)).from("s5").goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731667, 2717, 0)).from("s5").goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2717));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731667, 10005, 0)).from("s5").then(new QuestAction.SetVariable("var0", 6)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731668, -1, 0)).from("s6").goTo("s6");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3057));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731668, 3058, 0)).from("s6").goTo("s6");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3058));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731668, 10006, 0)).from("s6").then(new QuestAction.SetVariable("var0", 7)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("ID_ETERNITY_Q_SENSORYAREA_B_301570000")).from("s7").then(new QuestAction.SetVariable("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806136, 31, 0)).from("s8").goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3739));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806136, 3740, 0)).from("s8").goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3740));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806136, 10008, 0)).from("s8").then(new QuestAction.SetVariable("var0", 9)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.DeleteInteractionNpc(false));
		builder.afterCommit(new AfterCommitAction.StartInvisibleTimer(3, new QuestTimerPolicy(new QuestTimerPolicy.Identity("10521-leibo1-spawn", QuestTimerPolicy.Scope.PLAYER_QUEST), QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE, QuestTimerPolicy.Delivery.AT_MOST_ONCE)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.InvisibleTimerEnd()).from("s9").goTo("s9");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("sado-wi-1", 857948, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 446.12146f, 654.5927f, 468.97745f, (byte) 19)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("sado-wi-n", 857903, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 451.2063f, 654.0501f, 468.97745f, (byte) 20)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("sado-wi-2", 857948, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 453.82755f, 650.27997f, 468.97745f, (byte) 19)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731669, -1, 0)).from("s9").goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4080));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731669, 4081, 0)).from("s9").goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4081));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731669, 4166, 0)).from("s9").goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4166));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731669, 10009, 0)).from("s9").then(new QuestAction.SetVariable("var0", 10)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("ID_ETERNITY_Q_SENSORYAREA_C_301570000")).from("s10").then(new QuestAction.SetVariable("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806137, 31, 0)).from("s11").goTo("s11");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(6841));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806137, 6842, 0)).from("s11").goTo("s11");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(6842));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806137, 10011, 0)).from("s11").then(new QuestAction.SetVariable("var0", 12)).goTo("s12");
		builder.afterCommit(new AfterCommitAction.DeleteInteractionNpc(false));
		builder.afterCommit(new AfterCommitAction.StartInvisibleTimer(3, new QuestTimerPolicy(new QuestTimerPolicy.Identity("10521-leibo2-spawn", QuestTimerPolicy.Scope.PLAYER_QUEST), QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE, QuestTimerPolicy.Delivery.AT_MOST_ONCE)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.InvisibleTimerEnd()).from("s12").goTo("s12");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("cube-as-1", 857915, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 346.18872f, 516.0532f, 468.937f, (byte) 119)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("energy-wi", 857916, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 347.85834f, 511.8845f, 468.937f, (byte) 0)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("cube-as-2", 857915, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 346.09894f, 507.7084f, 468.937f, (byte) 119)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703130, -1, 0)).from("s12").goTo("s12");
		builder.afterCommit(new AfterCommitAction.PlayMovie(923));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.MovieEnd(923)).from("s12").when(new QuestCondition.GenderIs(Gender.MALE)).then(new QuestAction.SetVariable("var0", 13)).goTo("s13");
		builder.afterCommit(new AfterCommitAction.SendSystemMessagePacket(new QuestSystemMessagePacket(1403364, QuestSystemMessageTarget.NONE, false, 26, List.of())));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("wind-li", 857788, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 231.63109f, 511.9707f, 468.80215f, (byte) 0)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.MovieEnd(923)).from("s12").when(new QuestCondition.GenderIs(Gender.FEMALE)).then(new QuestAction.SetVariable("var0", 13)).goTo("s13");
		builder.afterCommit(new AfterCommitAction.SendSystemMessagePacket(new QuestSystemMessagePacket(1403364, QuestSystemMessageTarget.NONE, false, 26, List.of())));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("wind-li", 857795, new QuestSpawnLocation.Fixed(301570000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 231.63109f, 511.9707f, 468.80215f, (byte) 0)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(857785, 857792))).from("s13").then(new QuestAction.SetVariable("var0", 14)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 451980L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100201657, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100001994, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100101476, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100901511, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101301395, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101501497, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101701492, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100601552, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 100501434, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102001355, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101801327, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 101901232, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 102101170, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 187000186, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 166100009, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s3").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s4").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s5").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s6").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s7").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s8").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s9").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s10").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s11").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s12").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s13").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s3").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s4").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s5").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s6").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s7").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s8").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s9").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s10").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s11").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s12").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("s13").then(new QuestAction.SetVariable("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
