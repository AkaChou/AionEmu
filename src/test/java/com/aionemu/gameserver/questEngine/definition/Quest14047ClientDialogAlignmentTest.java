package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定任务 14047 的 Aion 5.8 客户端页面链、过场动画时序和副本任务 NPC 出生点。
 * Locks quest 14047 to the Aion 5.8 client page chain, cutscene order, and instance quest NPC spawns.
 */
class Quest14047ClientDialogAlignmentTest {
	private static final int PEITHO = 802052;
	private static final int ICARONIX_ENTRY_FORM = 233877;
	private static final int ICARONIX_KILL_FORM = 214599;
	private static final Path AZOTURAN_SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/310100000_Azoturan_Fortress.xml");

	@Test
	void routesEachVisibleContinuationActionThroughItsAuthoritativeNpcAndState() throws Exception {
		QuestDefinition definition = definition();

		assertPage(definition, "started", 0, 203704, QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertPage(definition, "s1", 1, 798154, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "s2", 2, 204574, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertPage(definition, "s3", 3, 802051, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
		assertPage(definition, "s6", 6, 802051, QuestDialogAction.SELECT7_1, QuestDialogPage.SELECT7_1);
	}

	@Test
	void returnsFromMovie421ToTheStep11PageAndThenAdvancesToStep5() throws Exception {
		QuestDefinition definition = definition();
		QuestTransition movie = talk(definition, "s4", PEITHO, QuestDialogAction.SELECT5_1);

		assertEquals("s4", movie.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 4)), node(definition, "s4").projection());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4)), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(421),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1.id())), movie.afterCommit());

		QuestTransition advance = talk(definition, "s4", PEITHO, QuestDialogAction.SETPRO11);
		assertEquals("s5", advance.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 5)), node(definition, "s5").projection());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4)), advance.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), advance.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog(),
			new AfterCommitAction.FlightTeleport(72001)), advance.afterCommit());
	}

	@Test
	void synchronizesCommittedProgressBeforeFlightsAndTheFinalMovie() throws Exception {
		QuestDefinition definition = definition();
		QuestTransition firstFlight = talk(definition, "s3", 802051, QuestDialogAction.SETPRO10);

		assertEquals("s4", firstFlight.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 3)), node(definition, "s3").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 4)), node(definition, "s4").projection());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), firstFlight.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), firstFlight.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog(),
			new AfterCommitAction.FlightTeleport(71001)), firstFlight.afterCommit());

		QuestTransition finalKill = definition.transitions().stream()
			.filter(transition -> "s5".equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(ICARONIX_KILL_FORM)))
			.findFirst().orElseThrow();
		assertEquals("s6", finalKill.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 5)), node(definition, "s5").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 6)), node(definition, "s6").projection());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 5)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), finalKill.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.PlayMovie(422)), finalKill.afterCommit());
	}

	@Test
	void opensTheRewardWindowAtJuditioInsteadOfTheMissingStep3Page() throws Exception {
		QuestDefinition definition = definition();
		QuestTransition rewardPreview = talk(definition, "reward", 278500, QuestDialogAction.USE_OBJECT);

		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 6)),
			node(definition, "reward").projection());
		assertEquals("reward", rewardPreview.targetNode());
		assertEquals(List.of(), rewardPreview.conditions());
		assertEquals(List.of(), rewardPreview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardPreview.afterCommit());
	}

	@Test
	void rollsBackUnreachableFlightStepsOnRelogin() throws Exception {
		QuestDefinition definition = definition();

		assertRecovery(definition, "s4", 4, "s3", 3, QuestEvent.EnterWorld.class);
		assertRecovery(definition, "s5", 5, "s3", 3, QuestEvent.EnterWorld.class);

		List<QuestTransition> recoveryTransitions = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.LogOut
				|| transition.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(2, recoveryTransitions.size());
	}

	@Test
	void closesWrongStageSelectionsInsteadOfSendingActionIdsAsPages() throws Exception {
		QuestDefinition definition = definition();

		assertClosedSelection(definition, 802051, Map.of(
			"started", 0,
			"s1", 1,
			"s2", 2,
			"s4", 4,
			"s5", 5));
		assertClosedSelection(definition, PEITHO, Map.of(
			"started", 0,
			"s1", 1,
			"s2", 2,
			"s3", 3,
			"s5", 5,
			"s6", 6));
	}

	@Test
	void keepsOnlyTheQuestPeithoAndSpawnsTheIcaronixEntryForm() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(AZOTURAN_SPAWNS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList peithoSpawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='310100000']/spawn[@npc_id='802052' or @npc_id='204653']",
			document, XPathConstants.NODESET);
		NodeList icaronixSpawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='310100000']/spawn[@npc_id='233877' or @npc_id='214599']",
			document, XPathConstants.NODESET);

		assertEquals(1, peithoSpawns.getLength());
		assertEquals(Integer.toString(PEITHO), ((Element) peithoSpawns.item(0)).getAttribute("npc_id"));
		assertEquals(1, icaronixSpawns.getLength());
		assertEquals(Integer.toString(ICARONIX_ENTRY_FORM), ((Element) icaronixSpawns.item(0)).getAttribute("npc_id"));
	}

	private static void assertPage(QuestDefinition definition, String source, int sourceVar, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action);
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", sourceVar)),
			node(definition, source).projection());
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertClosedSelection(QuestDefinition definition, int npcId,
		Map<String, Integer> sourceVariables) {
		for (Map.Entry<String, Integer> source : sourceVariables.entrySet()) {
			QuestTransition transition = talk(definition, source.getKey(), npcId, QuestDialogAction.QUEST_SELECT);
			assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", source.getValue())),
				node(definition, source.getKey()).projection());
			assertEquals(source.getKey(), transition.targetNode());
			assertEquals(List.of(), transition.conditions());
			assertEquals(List.of(), transition.actions());
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), transition.afterCommit());
		}
	}

	private static void assertRecovery(QuestDefinition definition, String source, int sourceVar,
		String target, int targetVar, Class<? extends QuestEvent> eventType) {
		QuestTransition recovery = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> eventType.isInstance(transition.event()))
			.findFirst().orElseThrow();
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", sourceVar)),
			node(definition, source).projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", targetVar)),
			node(definition, target).projection());
		assertEquals(List.of(), recovery.conditions());
		assertEquals(List.of(), recovery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			recovery.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest14047ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/14047.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14047.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
