package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定任务 10520/20520 的信件领取、重置清理、阅读推进、报告和传送时序。
 * Locks quest 10520/20520 letter handoff, reset cleanup, read progression, report, and teleport ordering.
 */
class Quest10520ClientDialogAlignmentTest {
	private static final int JUCLEAS = 203752;
	private static final int POLYIDUS = 203726;
	private static final int BALDER = 204075;
	private static final int DOMAN = 204191;

	@Test
	void receivingTheSealedLetterDoesNotTeleportEarly() throws Exception {
		QuestDefinition definition = load().definition();
		QuestTransition receive = route(definition, "started", JUCLEAS, QuestDialogAction.SETPRO1);

		assertEquals("s1", receive.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 0)),
			node(definition, "started").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			node(definition, "s1").projection());
		assertEquals(List.of(), receive.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 1),
			new QuestAction.GiveItem(182215973, 1)), receive.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), receive.afterCommit());
	}

	@Test
	void readingTheSealedLetterAdvancesTheQuestAndKeepsTheClientReadAction() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(List.of(), definition.metadata().itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(182215973, 1)),
			definition.metadata().questWorkItems());
		QuestTransition read = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s1"))
			.filter(transition -> transition.event() instanceof QuestEvent.UseItem use
				&& use.itemId() == 182215973)
			.findFirst().orElseThrow();

		assertEquals("s2", read.targetNode());
		assertEquals(new QuestEvent.UseItem(182215973), read.event());
		assertNull(read.priority());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			node(definition, "s1").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 2)),
			node(definition, "s2").projection());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), read.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 2),
			new QuestAction.RemoveItem(182215973, 1)), read.actions());
		assertFalse(read.actions().stream().anyMatch(QuestAction.BlockDefaultItemUse.class::isInstance));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			read.afterCommit());
	}

	@Test
	void asmodianSealedLetterIsAlsoRegisteredAsAQuestWorkItem() throws Exception {
		QuestDefinition definition = load(20520).definition();

		assertEquals(List.of(), definition.metadata().itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(182215974, 1)),
			definition.metadata().questWorkItems());
		QuestTransition receive = route(definition, "started", BALDER, QuestDialogAction.SETPRO1);
		assertEquals("s1", receive.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), receive.afterCommit());
	}

	@Test
	void reportsTheStampedLetterToPolyidusAndTeleportsOnlyAfterCompletion() throws Exception {
		QuestDefinition definition = load().definition();
		List<QuestTransition> transitions = definition.transitions();

		QuestTransition openReport = route(definition, "s3", POLYIDUS, QuestDialogAction.QUEST_SELECT);
		assertEquals("s3", openReport.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			openReport.afterCommit());

		QuestTransition reportPage = route(definition, "s3", POLYIDUS, QuestDialogAction.SELECT4_1);
		assertEquals("s3", reportPage.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_1.id())),
			reportPage.afterCommit());

		QuestTransition report = route(definition, "s3", POLYIDUS, QuestDialogAction.SETPRO4);
		assertEquals("s4", report.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 3)),
			node(definition, "s3").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 4)),
			node(definition, "s4").projection());
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 4),
			new QuestAction.RemoveItem(182215953, 1)), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog(),
			new AfterCommitAction.TeleportPlayer(210100000, 1456.6283f, 1299.3306f, 336.49023f, (byte) 8)),
			report.afterCommit());
		assertFalse(transitions.stream().anyMatch(transition -> transition.sourceNode().equals("s3")
			&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == JUCLEAS));
	}

	@Test
	void asmodianReportTeleportsOnlyAfterStateAndDialogPackets() throws Exception {
		QuestDefinition definition = load(20520).definition();
		QuestTransition report = route(definition, "s3", DOMAN, QuestDialogAction.SETPRO4);

		assertEquals("s4", report.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 3)),
			node(definition, "s3").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 4)),
			node(definition, "s4").projection());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 4),
			new QuestAction.RemoveItem(182215954, 1)), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog(),
			new AfterCommitAction.TeleportPlayer(220110000, 1757.3667f, 2008.911f, 196.59653f, (byte) 0)),
			report.afterCommit());
	}

	@Test
	void broadcastsOnlyTypedFollowUpMissionOwners() throws Exception {
		QuestDefinition definition = load().definition();
		assertArrayEquals(new int[]{10521, 10522, 10525, 10526, 10527, 10528, 10529, 10530},
			broadcastTargets(definition, 806076).questIds());
	}

	@Test
	void asmodianCompanionAlsoBroadcastsOnlyTypedFollowUpMissionOwners() throws Exception {
		QuestDefinition definition = load(20520).definition();

		assertArrayEquals(new int[]{20521, 20522, 20525, 20526, 20527, 20528, 20529, 20530},
			broadcastTargets(definition, 806080).questIds());
	}

	private static AfterCommitAction.BroadcastZoneMissionEnd broadcastTargets(QuestDefinition definition,
			int npcId) {
		QuestTransition missionEnd = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("reward")
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, QuestDialogAction.USE_OBJECT.id())))
			.findFirst().orElseThrow();
		return missionEnd.afterCommit().stream()
			.filter(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance)
			.map(AfterCommitAction.BroadcastZoneMissionEnd.class::cast)
			.findFirst().orElseThrow();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> node.label().equals(label))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition load() throws Exception {
		return load(10520);
	}

	private CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
