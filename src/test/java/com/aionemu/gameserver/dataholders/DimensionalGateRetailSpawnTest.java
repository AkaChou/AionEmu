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
 * 锁定两个次元之门副本只保留真端的单点 NPC 出生位置。
 * Locks the dimensional-gate instances to the retail single-point NPC spawns.
 */
class DimensionalGateRetailSpawnTest {

	private static final Path SPAWN_ROOT = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances");

	@Test
	void keepsOnlyRetailSinglePointNpcSpawns() throws Exception {
		assertSingleSpawn("310040000_Geranaia.xml", "310040000", "233878",
			"256.375580", "243.207993", "229.273560");
		assertSingleSpawn("310040000_Geranaia.xml", "310040000", "204044",
			"272.664032", "175.631027", "207.000000");
		assertSingleSpawn("320040000_Nidalber.xml", "320040000", "233879",
			"258.531769", "239.821655", "231.518051");
		assertSingleSpawn("320040000_Nidalber.xml", "320040000", "204432",
			"272.828857", "176.814392", "207.000000");
	}

	private static void assertSingleSpawn(String fileName, String mapId, String npcId,
		String expectedX, String expectedY, String expectedZ) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(SPAWN_ROOT.resolve(fileName).toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList spawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='" + mapId + "']/spawn[@npc_id='" + npcId + "']",
			document, XPathConstants.NODESET);

		assertEquals(1, spawns.getLength());
		NodeList spots = ((Element) spawns.item(0)).getElementsByTagName("spot");
		assertEquals(1, spots.getLength());

		Element spot = (Element) spots.item(0);
		assertEquals(expectedX, spot.getAttribute("x"));
		assertEquals(expectedY, spot.getAttribute("y"));
		assertEquals(expectedZ, spot.getAttribute("z"));
	}
}
