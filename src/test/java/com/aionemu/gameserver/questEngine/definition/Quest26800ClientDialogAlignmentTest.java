package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定任务 26800 的 Aion 5.8 客户端页面、跨地图阶段和最终领奖 owner。
 * Locks quest 26800 to the Aion 5.8 client pages, cross-map stages, and final reward owner.
 */
class Quest26800ClientDialogAlignmentTest {
	private static final int START_NPC = 806079;
	private static final int HANDOFF_NPC = 806233;
	private static final int REWARD_NPC = 806149;
	private static final int QUEST_20527_FRAGMENT = 731711;
	private static final int TOWER_PORTAL = 806082;
	private static final int ARCHIVES_PORTAL = 806029;
	private static final Path PORTAL_TEMPLATES = Path.of(
		"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path PORTAL_LOCATIONS = Path.of(
		"src/main/resources/aion/data/static_data/portals/portal_loc.xml");

	@Test
	void followsTheAion58SimpleAcceptPageChain() throws Exception {
		QuestDefinition definition = definition(26800);

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "s1", QuestStatus.START, 1);
		assertNode(definition, "s2", QuestStatus.START, 2);
		assertNode(definition, "reward", QuestStatus.REWARD, 3);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT_NONE);
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.SELECT_NONE_1,
			QuestDialogPage.SELECT_NONE_1);

		QuestTransition accept = talk(definition, "unaccepted", "started", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
	}

	@Test
	void advancesThroughTheTowerAndEnfitentaOnlyAtTheAuthoritativeStages() throws Exception {
		QuestDefinition definition = definition(26800);

		QuestTransition towerArrival = transition(definition, "started", "s1",
			new QuestEvent.EnterZone("DF_TOWER_SENSORY_AREA_Q26800_220120000"));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), towerArrival.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), towerArrival.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			towerArrival.afterCommit());

		assertPage(definition, "s1", HANDOFF_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPage(definition, "s1", HANDOFF_NPC, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);

		QuestTransition handoff = talk(definition, "s1", "s2", HANDOFF_NPC,
			QuestDialogAction.SET_SUCCEED);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), handoff.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), handoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());
	}

	@Test
	void entersRewardBeforeMovieAndKeepsFereganAsTheOnlyRewardOwner() throws Exception {
		QuestDefinition definition = definition(26800);

		QuestTransition archivesArrival = transition(definition, "s2", "reward",
			new QuestEvent.EnterZone("IDETERNITY_01_Q16800_301540000"));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), archivesArrival.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), archivesArrival.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.PlayMovie(932)), archivesArrival.afterCommit());

		assertPage(definition, "reward", REWARD_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		QuestTransition preview = talk(definition, "reward", "reward", REWARD_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", REWARD_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 155160, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 8868125, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());

		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		assertTrue(routes(definition, "reward", HANDOFF_NPC).isEmpty());
	}

	@Test
	void keepsTheQuest20527FragmentSeparateFromTheTwoWorldPortals() throws Exception {
		QuestDefinition quest26800 = definition(26800);
		QuestDefinition quest20527 = definition(20527);

		assertFalse(hasNpcEvent(quest26800, QUEST_20527_FRAGMENT));
		assertTrue(quest20527.transitions().stream()
			.anyMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(QUEST_20527_FRAGMENT, QuestDialogAction.USE_OBJECT.id()))));

		Document templates = document(PORTAL_TEMPLATES);
		Element tower = element(templates,
			"/portal_templates2/portal_dialog[@npc_id='" + TOWER_PORTAL + "']/portal_path");
		assertEquals("104", tower.getAttribute("dialog"));
		assertEquals("2201200", tower.getAttribute("loc_id"));

		Element archives = element(templates,
			"/portal_templates2/portal_dialog[@npc_id='" + ARCHIVES_PORTAL + "']/portal_path");
		assertEquals("10000", archives.getAttribute("dialog"));
		assertEquals("3015400", archives.getAttribute("loc_id"));
		assertEquals("true", archives.getAttribute("instance"));
		assertEquals("ASMODIANS", archives.getAttribute("race"));

		Document locations = document(PORTAL_LOCATIONS);
		assertEquals("220120000", element(locations,
			"/portal_locs/portal_loc[@loc_id='2201200']").getAttribute("world_id"));
		assertEquals("301540000", element(locations,
			"/portal_locs/portal_loc[@loc_id='3015400']").getAttribute("world_id"));
	}

	private static boolean hasNpcEvent(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition -> switch (transition.event()) {
			case QuestEvent.TalkToNpc talk -> talk.npcId() == npcId;
			case QuestEvent.CanAct canAct -> canAct.templateId() == npcId;
			default -> false;
		});
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, source, npcId, action);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 26800 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(new NodeProjection(status, Map.of("var0", var0)), node.projection());
	}

	private static Document document(Path path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(path.toFile());
	}

	private static Element element(Document document, String expression) throws Exception {
		NodeList matches = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
			expression, document, XPathConstants.NODESET);
		assertEquals(1, matches.getLength(), expression);
		return (Element) matches.item(0);
	}

	private static QuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Quest26800ClientDialogAlignmentTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
