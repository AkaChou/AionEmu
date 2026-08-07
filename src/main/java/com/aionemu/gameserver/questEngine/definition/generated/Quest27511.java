package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest27511 {
	private Quest27511() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(27511)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("The Avatars", 1802762, 66, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 150000L), new QuestReward("EXP", 0, 51840000L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 12, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 4, 3, 0, 5, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("s5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("s8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 8))));
		builder.node("s9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 9))));
		builder.node("s10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 11))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("unaccepted").when(new QuestCondition.WorldIs(302100000, true)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(244633, 244632, 244631, 244630, 244629, 244628, 244627, 244626, 244625, 244624, 244623, 244622, 244592, 244588, 244589, 244590, 244591, 244587, 244838, 244837, 244836, 244835, 244834, 244833, 244832, 244831, 244830, 244829, 244828, 244827, 244586, 244585, 244584, 244583, 244582, 244581, 244548, 244549, 244550, 244551, 244546, 244547, 244797, 244796, 244795, 244794, 244793, 244792, 244791, 244790, 244789, 244788, 244787, 244786, 244545, 244544, 244543, 244542, 244541, 244540, 244508, 244509, 244510, 244505, 244506, 244507, 244756, 244755, 244754, 244753, 244752, 244751, 244750, 244749, 244748, 244747, 244746, 244745, 244504, 244503, 244502, 244501, 244500, 244499, 244468, 244469, 244464, 244465, 244466, 244467, 244715, 244714, 244713, 244712, 244711, 244710, 244709, 244708, 244707, 244706, 244705, 244704, 244463, 244462, 244461, 244460, 244459, 244458, 244674, 244673, 244672, 244671, 244670, 244669, 244668, 244667, 244666, 244665, 244664, 244663))).from("started").priority(0).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.VariableAtLeast("var1", 4)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(244633, 244632, 244631, 244630, 244629, 244628, 244627, 244626, 244625, 244624, 244623, 244622, 244592, 244588, 244589, 244590, 244591, 244587, 244838, 244837, 244836, 244835, 244834, 244833, 244832, 244831, 244830, 244829, 244828, 244827, 244586, 244585, 244584, 244583, 244582, 244581, 244548, 244549, 244550, 244551, 244546, 244547, 244797, 244796, 244795, 244794, 244793, 244792, 244791, 244790, 244789, 244788, 244787, 244786, 244545, 244544, 244543, 244542, 244541, 244540, 244508, 244509, 244510, 244505, 244506, 244507, 244756, 244755, 244754, 244753, 244752, 244751, 244750, 244749, 244748, 244747, 244746, 244745, 244504, 244503, 244502, 244501, 244500, 244499, 244468, 244469, 244464, 244465, 244466, 244467, 244715, 244714, 244713, 244712, 244711, 244710, 244709, 244708, 244707, 244706, 244705, 244704, 244463, 244462, 244461, 244460, 244459, 244458, 244674, 244673, 244672, 244671, 244670, 244669, 244668, 244667, 244666, 244665, 244664, 244663))).from("started").priority(1).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.VariableBelow("var1", 5)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("IDTRANSFORM_Q17511_A_302100000")).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 4)).then(new QuestAction.SetVariable("var1", 0)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(245600, 245599, 245598, 245597, 245596, 245595, 245594, 245593, 245592, 245591, 245590, 245589, 245588, 245587, 245586, 245585, 245584, 245583, 245582, 245581, 245580, 245579, 245578, 245577, 245696, 245695, 245694, 245693, 245692, 245691, 245690, 245689, 245688, 245687, 245686, 245685, 245684, 245683, 245682, 245681, 245680, 245679, 245678, 245677, 245676, 245675, 245674, 245673, 245672, 245671, 245670, 245669, 245668, 245667, 245666, 245665, 245664, 245663, 245662, 245661, 245660, 245659, 245658, 245657, 245656, 245655, 245654, 245653, 245652, 245651, 245650, 245649, 245648, 245647, 245646, 245645, 245644, 245643, 245642, 245641, 245640, 245639, 245638, 245637, 245636, 245635, 245634, 245633, 245632, 245631, 245630, 245629, 245628, 245627, 245626, 245625, 245624, 245623, 245622, 245621, 245620, 245619, 245618, 245617, 245616, 245615, 245614, 245613, 245612, 245611, 245610, 245609, 245608, 245607, 245606, 245605, 245604, 245603, 245602, 245601))).from("s4").priority(0).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.VariableAtLeast("var1", 4)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(245600, 245599, 245598, 245597, 245596, 245595, 245594, 245593, 245592, 245591, 245590, 245589, 245588, 245587, 245586, 245585, 245584, 245583, 245582, 245581, 245580, 245579, 245578, 245577, 245696, 245695, 245694, 245693, 245692, 245691, 245690, 245689, 245688, 245687, 245686, 245685, 245684, 245683, 245682, 245681, 245680, 245679, 245678, 245677, 245676, 245675, 245674, 245673, 245672, 245671, 245670, 245669, 245668, 245667, 245666, 245665, 245664, 245663, 245662, 245661, 245660, 245659, 245658, 245657, 245656, 245655, 245654, 245653, 245652, 245651, 245650, 245649, 245648, 245647, 245646, 245645, 245644, 245643, 245642, 245641, 245640, 245639, 245638, 245637, 245636, 245635, 245634, 245633, 245632, 245631, 245630, 245629, 245628, 245627, 245626, 245625, 245624, 245623, 245622, 245621, 245620, 245619, 245618, 245617, 245616, 245615, 245614, 245613, 245612, 245611, 245610, 245609, 245608, 245607, 245606, 245605, 245604, 245603, 245602, 245601))).from("s4").priority(1).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.VariableBelow("var1", 5)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("IDTRANSFORM_Q17511_B_302100000")).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 8)).then(new QuestAction.SetVariable("var1", 0)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(244592, 244591, 244590, 244589, 244588, 244587, 244586, 244585, 244584, 244583, 244582, 244581, 245752, 245753, 245751, 245745, 245746, 245747, 245740, 245741, 245739, 245733, 245734, 245735, 245728, 245729, 244551, 244550, 244549, 244548, 244547, 244546, 244545, 244544, 244543, 244542, 244541, 244540, 245727, 244838, 244837, 244836, 244835, 244834, 244833, 244832, 244831, 244830, 244829, 244828, 244827, 245721, 245723, 245722, 245716, 245717, 245715, 245709, 245710, 245711, 245704, 245705, 245703, 245697, 245699, 244510, 244509, 244508, 244507, 244506, 244505, 244504, 244503, 244502, 244501, 244500, 244499, 245698, 244797, 244796, 244795, 244794, 244793, 244792, 244791, 244790, 244789, 244788, 244787, 244786, 244469, 244468, 244467, 244466, 244465, 244464, 244463, 244462, 244461, 244460, 244459, 244458, 244756, 244755, 244754, 244753, 244752, 244751, 244750, 244749, 244748, 244747, 244746, 244745, 244715, 244714, 244713, 244712, 244711, 244710, 244709, 244708, 244707, 244706, 244705, 244704, 244674, 244673, 244672, 244671, 244670, 244669, 244668, 244667, 244666, 244665, 244664, 244663, 244633, 244632, 244631, 244630, 244629, 244628, 244627, 244626, 244625, 244624, 244623, 244622))).from("s8").priority(0).when(new QuestCondition.QuestVariableIs("var0", 8)).when(new QuestCondition.VariableAtLeast("var1", 4)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(244592, 244591, 244590, 244589, 244588, 244587, 244586, 244585, 244584, 244583, 244582, 244581, 245752, 245753, 245751, 245745, 245746, 245747, 245740, 245741, 245739, 245733, 245734, 245735, 245728, 245729, 244551, 244550, 244549, 244548, 244547, 244546, 244545, 244544, 244543, 244542, 244541, 244540, 245727, 244838, 244837, 244836, 244835, 244834, 244833, 244832, 244831, 244830, 244829, 244828, 244827, 245721, 245723, 245722, 245716, 245717, 245715, 245709, 245710, 245711, 245704, 245705, 245703, 245697, 245699, 244510, 244509, 244508, 244507, 244506, 244505, 244504, 244503, 244502, 244501, 244500, 244499, 245698, 244797, 244796, 244795, 244794, 244793, 244792, 244791, 244790, 244789, 244788, 244787, 244786, 244469, 244468, 244467, 244466, 244465, 244464, 244463, 244462, 244461, 244460, 244459, 244458, 244756, 244755, 244754, 244753, 244752, 244751, 244750, 244749, 244748, 244747, 244746, 244745, 244715, 244714, 244713, 244712, 244711, 244710, 244709, 244708, 244707, 244706, 244705, 244704, 244674, 244673, 244672, 244671, 244670, 244669, 244668, 244667, 244666, 244665, 244664, 244663, 244633, 244632, 244631, 244630, 244629, 244628, 244627, 244626, 244625, 244624, 244623, 244622))).from("s8").priority(1).when(new QuestCondition.QuestVariableIs("var0", 8)).when(new QuestCondition.VariableBelow("var1", 5)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("IDTRANSFORM_Q17511_C_302100000")).from("s9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(244532, 244531, 244822, 244821, 244820, 244819, 244818, 244617, 244616, 244615, 244614, 244613, 244699, 244698, 244697, 244696, 244695, 244494, 244493, 244492, 244491, 244490, 244781, 244780, 244779, 244778, 244777, 244576, 244575, 244574, 244573, 244572, 244863, 244862, 244861, 244860, 244859, 244658, 244657, 244656, 244655, 244654, 244740, 244739, 244738, 244737, 244736, 244535, 244534, 244533))).from("s10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 150000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 51840000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
