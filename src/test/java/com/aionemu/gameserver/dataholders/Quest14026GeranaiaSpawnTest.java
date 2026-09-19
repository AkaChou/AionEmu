package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定任务 14026 在 Geranaia 中只有一个任务 Kimeia 出生点。
 * Locks quest 14026 to one quest Kimeia spawn point in Geranaia.
 */
class Quest14026GeranaiaSpawnTest {

	private static final Path GERANAIA_SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/310040000_Geranaia.xml");
	private static final Path QUEST_14026 = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/14026.xml");
	private static final Path NPC_AI = Path.of("src/main/resources/aion/definitions/compact/ai/npc-ai.xml");
	private static final String[][] DEFENSE_VARIANTS = {
		{"213576", "254.74", "236.72", "217.48"},
		{"213577", "257.92", "237.39", "217.48"},
		{"213578", "261.86", "237.5", "217.48"}
	};

	@Test
	void keepsTheRetailQuestKimeiaAtTheGeranaiaEntry() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(GERANAIA_SPAWNS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList spawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='310040000']/spawn[@npc_id='204044']",
			document, XPathConstants.NODESET);

		assertEquals(1, spawns.getLength());
		NodeList spots = ((Element) spawns.item(0)).getElementsByTagName("spot");
		assertEquals(1, spots.getLength());

		Element spot = (Element) spots.item(0);
		assertEquals("272.664032", spot.getAttribute("x"));
		assertEquals("175.631027", spot.getAttribute("y"));
		assertEquals("207.000000", spot.getAttribute("z"));
	}

	@Test
	void keepsLegacyDefensePositionsAndUsesUnlimitedChaseTime() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var builder = factory.newDocumentBuilder();
		var document = builder.parse(QUEST_14026.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		String defenseVariantPath = "/quest-definition[@id='14026']/transitions/transition"
			+ "[(@source='s2' and @target='defense') or (@source='defense' and @target='defense')]"
			+ "/after-commit/spawn-npc-random[@slot='defense-mob']/variant";
		NodeList variants = (NodeList) xpath.evaluate(
			defenseVariantPath,
			document, XPathConstants.NODESET);

		assertEquals(DEFENSE_VARIANTS.length * 2, variants.getLength());
		for (int index = 0; index < variants.getLength(); index++) {
			Element variant = (Element) variants.item(index);
			String[] expected = DEFENSE_VARIANTS[index % DEFENSE_VARIANTS.length];
			assertEquals(expected[0], variant.getAttribute("template-id"));
			assertEquals(expected[1], variant.getAttribute("x"));
			assertEquals(expected[2], variant.getAttribute("y"));
			assertEquals(expected[3], variant.getAttribute("z"));
		}

		var aiDocument = builder.parse(NPC_AI.toFile());
		NodeList aiDefinitions = (NodeList) xpath.evaluate(
			"/npc_ai_mappings/npc[@id='213576' or @id='213577' or @id='213578' or @id='213579']",
			aiDocument, XPathConstants.NODESET);
		assertEquals(4, aiDefinitions.getLength());
		for (int index = 0; index < aiDefinitions.getLength(); index++) {
			assertEquals("0", ((Element) aiDefinitions.item(index)).getAttribute("max_chase_time"));
		}
	}
}
