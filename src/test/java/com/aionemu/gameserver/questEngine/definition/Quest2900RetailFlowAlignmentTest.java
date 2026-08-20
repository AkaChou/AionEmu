package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 2900 在命运空间中的黑利温击杀推进和提交后动作顺序。
 * Locks quest 2900's Hellion kill progression, unique Skuld spawn, and post-commit action order
 * in the Space of Destiny.
 */
class Quest2900RetailFlowAlignmentTest {
	private static final int HELLION = 204263;
	private static final int EXIT_WORLD = 220010000;
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/320070000_Space_Of_Destiny.xml");

	@Test
	void advancesAfterHellionDeathBeforeLeavingTheInstance() {
		QuestDefinition definition = load().definition();
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(transition -> "fight98".equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(HELLION)))
			.toList();
		assertEquals(1, matches.size());
		QuestTransition kill = matches.getFirst();

		assertEquals("postFight9", kill.targetNode());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("step", 98)),
			node(definition, "fight98").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("step", 9)),
			node(definition, "postFight9").projection());
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("step", 9)), kill.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.TeleportPlayer(EXIT_WORLD, 1103.5642f, 1708.5078f, 270.05505f, (byte) 112)),
			kill.afterCommit());
	}

	@Test
	void keepsOneSkuldSpawnAtTheOriginalInstanceAnchor() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(SPAWNS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList spawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='320070000']/spawn[@npc_id='204264']",
			document, XPathConstants.NODESET);

		assertEquals(1, spawns.getLength());
		NodeList spots = ((Element) spawns.item(0)).getElementsByTagName("spot");
		assertEquals(1, spots.getLength());
		Element spot = (Element) spots.item(0);
		assertEquals("245.815", spot.getAttribute("x"));
		assertEquals("248.099", spot.getAttribute("y"));
		assertEquals("125.837", spot.getAttribute("z"));
		assertEquals("51", spot.getAttribute("h"));
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/2900.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest2900RetailFlowAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
