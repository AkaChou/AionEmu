package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15515 {
	private Quest15515() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15515)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Daily] Protect Black Wind Valley", 1802096, 70, 2147483647, Set.of("ELYOS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 85792608L), new QuestReward("ITEM", 188054912, 1L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 15550, 0)), Map.of());
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
		builder.on(new QuestEvent.TalkToNpc(806094, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806094, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 85792608L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(240778, 240777, 239007, 239006, 239005, 239004, 239003, 239002, 239001, 239000, 238999, 238998, 238997, 238996, 238995, 238994, 238993, 238992, 238991, 238990, 238989, 238988, 238987, 238986, 238985, 238984, 240532, 240531, 240530, 240529, 240528, 240527, 239602, 239601, 239600, 239599, 239598, 239597, 239596, 239595, 239594, 239593, 239592, 239591, 239590, 239589, 239588, 239587, 239586, 239585, 239584, 239583, 239582, 239581, 239580, 239579, 239578, 239577, 239576, 239575, 239574, 239573, 239572, 239571, 239570, 239569, 239568, 239567, 239566, 239565, 239564, 239563, 239562, 239561, 239560, 239559, 239558, 239557, 239556, 239555, 239554, 239553, 239552, 239551, 239550, 239549, 239548, 239547, 239546, 239545, 239544, 239543, 239542, 239541, 239540, 239539, 239538, 239537, 239536, 239535, 239534, 239533, 239532, 239531, 239530, 239529, 239528, 239527, 239526, 239525, 239524, 239523, 239522, 239521, 239520, 239519, 239518, 239517, 239516, 239515, 239514, 239513, 239512, 239511, 239510, 239509, 239508, 239507, 239506, 239505, 239504, 239503, 239502, 239501, 239500, 239499, 239498, 239497, 239496, 239495, 239494, 239493, 239492, 239491, 239490, 239489, 239488, 239487, 239486, 239485, 239484, 239483, 239482, 239481, 239480, 239479, 239478, 239477, 239476, 239475, 239474, 239473, 239472, 239471, 239470, 239469, 239468, 239467, 239466, 239465, 239464, 239463, 239462, 239461, 239460, 239459, 242803, 242802, 242801, 242800, 242799, 242798, 242797, 242796, 242795, 242794, 242793, 242792, 242791, 242790, 242789, 242788, 242787, 242786, 242785, 242784, 242783, 242782, 242781, 242780, 242779, 242778, 242777, 242776, 242775, 242774, 242773, 242772, 242771, 242770, 242769, 242768, 242767, 242766, 242765, 242764, 242763, 242762, 242761, 242760, 242759, 242758, 242757, 242756, 242755, 242754, 242753, 242752, 242751, 242750, 242749, 242748, 242747, 242746, 242745, 242744, 242743, 242742, 242741, 242740, 242739, 242738, 242737, 242736, 242735, 242734, 242733, 242732, 242731, 242730, 242729, 242728, 242727, 242726, 242725, 242724, 243556, 243557, 243558, 243552, 243553, 243554, 243555, 243548, 243549, 243550, 243551, 243544, 243545, 243546, 243547, 243540, 243541, 243542, 243543, 243536, 243537, 243538, 243539, 243532, 243533, 243534, 243535, 243528, 243529, 243530, 243531, 243524, 243525, 243526, 243527, 243520, 243521, 243522, 243523, 243516, 243517, 243518, 243519, 243512, 243513, 243514, 243515, 243508, 243509, 243510, 243511, 243504, 243505, 243506, 243507, 243500, 243501, 243502, 243503, 243496, 243497, 243498, 243499, 243492, 243493, 243494, 243495, 243488, 243489, 243490, 243491, 243484, 243485, 243486, 243487, 243480, 243481, 243482, 243483, 243476, 243477, 243478, 243479, 243472, 243473, 243474, 243475, 243468, 243469, 243470, 243471, 243464, 243465, 243466, 243467, 243460, 243461, 243462, 243463, 243456, 243457, 243458, 243459, 243452, 243453, 243454, 243455, 243448, 243449, 243450, 243451, 243444, 243445, 243446, 243447, 243440, 243441, 243442, 243443, 243437, 243438, 243439, 241736, 241735, 241734, 241733, 241732, 241731, 241730, 241729, 241728, 241727, 241726, 241725, 243436, 243435, 243434, 243433, 243432, 243431, 243430, 243429, 243428, 243427, 243426, 243425, 243424, 243423, 243422, 243421, 243420, 243419, 243418, 243417, 243416, 243415, 243414, 243413, 243412, 243411, 243410, 243409, 243408, 243407, 243406, 243405, 243404, 243403, 243402, 243401, 243400, 243399))).from("started").priority(1).when(new QuestCondition.VariableBelow("var1", 29)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(240778, 240777, 239007, 239006, 239005, 239004, 239003, 239002, 239001, 239000, 238999, 238998, 238997, 238996, 238995, 238994, 238993, 238992, 238991, 238990, 238989, 238988, 238987, 238986, 238985, 238984, 240532, 240531, 240530, 240529, 240528, 240527, 239602, 239601, 239600, 239599, 239598, 239597, 239596, 239595, 239594, 239593, 239592, 239591, 239590, 239589, 239588, 239587, 239586, 239585, 239584, 239583, 239582, 239581, 239580, 239579, 239578, 239577, 239576, 239575, 239574, 239573, 239572, 239571, 239570, 239569, 239568, 239567, 239566, 239565, 239564, 239563, 239562, 239561, 239560, 239559, 239558, 239557, 239556, 239555, 239554, 239553, 239552, 239551, 239550, 239549, 239548, 239547, 239546, 239545, 239544, 239543, 239542, 239541, 239540, 239539, 239538, 239537, 239536, 239535, 239534, 239533, 239532, 239531, 239530, 239529, 239528, 239527, 239526, 239525, 239524, 239523, 239522, 239521, 239520, 239519, 239518, 239517, 239516, 239515, 239514, 239513, 239512, 239511, 239510, 239509, 239508, 239507, 239506, 239505, 239504, 239503, 239502, 239501, 239500, 239499, 239498, 239497, 239496, 239495, 239494, 239493, 239492, 239491, 239490, 239489, 239488, 239487, 239486, 239485, 239484, 239483, 239482, 239481, 239480, 239479, 239478, 239477, 239476, 239475, 239474, 239473, 239472, 239471, 239470, 239469, 239468, 239467, 239466, 239465, 239464, 239463, 239462, 239461, 239460, 239459, 242803, 242802, 242801, 242800, 242799, 242798, 242797, 242796, 242795, 242794, 242793, 242792, 242791, 242790, 242789, 242788, 242787, 242786, 242785, 242784, 242783, 242782, 242781, 242780, 242779, 242778, 242777, 242776, 242775, 242774, 242773, 242772, 242771, 242770, 242769, 242768, 242767, 242766, 242765, 242764, 242763, 242762, 242761, 242760, 242759, 242758, 242757, 242756, 242755, 242754, 242753, 242752, 242751, 242750, 242749, 242748, 242747, 242746, 242745, 242744, 242743, 242742, 242741, 242740, 242739, 242738, 242737, 242736, 242735, 242734, 242733, 242732, 242731, 242730, 242729, 242728, 242727, 242726, 242725, 242724, 243556, 243557, 243558, 243552, 243553, 243554, 243555, 243548, 243549, 243550, 243551, 243544, 243545, 243546, 243547, 243540, 243541, 243542, 243543, 243536, 243537, 243538, 243539, 243532, 243533, 243534, 243535, 243528, 243529, 243530, 243531, 243524, 243525, 243526, 243527, 243520, 243521, 243522, 243523, 243516, 243517, 243518, 243519, 243512, 243513, 243514, 243515, 243508, 243509, 243510, 243511, 243504, 243505, 243506, 243507, 243500, 243501, 243502, 243503, 243496, 243497, 243498, 243499, 243492, 243493, 243494, 243495, 243488, 243489, 243490, 243491, 243484, 243485, 243486, 243487, 243480, 243481, 243482, 243483, 243476, 243477, 243478, 243479, 243472, 243473, 243474, 243475, 243468, 243469, 243470, 243471, 243464, 243465, 243466, 243467, 243460, 243461, 243462, 243463, 243456, 243457, 243458, 243459, 243452, 243453, 243454, 243455, 243448, 243449, 243450, 243451, 243444, 243445, 243446, 243447, 243440, 243441, 243442, 243443, 243437, 243438, 243439, 241736, 241735, 241734, 241733, 241732, 241731, 241730, 241729, 241728, 241727, 241726, 241725, 243436, 243435, 243434, 243433, 243432, 243431, 243430, 243429, 243428, 243427, 243426, 243425, 243424, 243423, 243422, 243421, 243420, 243419, 243418, 243417, 243416, 243415, 243414, 243413, 243412, 243411, 243410, 243409, 243408, 243407, 243406, 243405, 243404, 243403, 243402, 243401, 243400, 243399))).from("started").priority(0).when(new QuestCondition.VariableAtLeast("var1", 29)).then(new QuestAction.SetVariable("var1", 30)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
