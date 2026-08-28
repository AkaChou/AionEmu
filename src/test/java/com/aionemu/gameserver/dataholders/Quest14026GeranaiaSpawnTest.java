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
}
