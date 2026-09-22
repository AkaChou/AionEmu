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
 * 永恒档案库任务副本惩罚者麦孜莱姆的真端出生面闸门。
 * Gate for the retail spawn surface of Crystalized Shardgolem in the Archives Of Eternity quest instance.
 * <p>背景：任务副本 301570000 曾同时保留手写 legacy 出生点与真端出生面，
 * 导致 857783 刷出 2 个。真端 {@code IDEternity_Q/world_N.xml} 中
 * {@code BIDEternity_Q_Sado_Fi_N_65_An_01} 只有 1 条 count=1 的出生记录，位置等于保留块。</p>
 */
class ArchivesOfEternityQSadoFiSpawnSurfaceTest {

	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/301570000_Archives_Of_Eternity.xml");
	private static final String NPC_ID = "857783";
	private static final String RETAIL_X = "544.977173";
	private static final String RETAIL_Y = "339.557068";
	private static final String RETAIL_Z = "469.500000";
	private static final String LEGACY_X = "552.93384";

	@Test
	void shardgolemSpawnsExactlyOnceAtTheRetailSurface() throws Exception {
		var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(SPAWNS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();

		NodeList spawns = (NodeList) xpath.evaluate(
			"/spawns/spawn_map[@map_id='301570000']/spawn[@npc_id='" + NPC_ID + "']",
			document, XPathConstants.NODESET);
		assertEquals(1, spawns.getLength(), NPC_ID + " must have exactly one spawn block");

		NodeList spots = ((Element) spawns.item(0)).getElementsByTagName("spot");
		assertEquals(1, spots.getLength(), NPC_ID + " must have exactly one spawn point");

		Element spot = (Element) spots.item(0);
		assertEquals(RETAIL_X, spot.getAttribute("x"), NPC_ID + " x must match the retail surface");
		assertEquals(RETAIL_Y, spot.getAttribute("y"), NPC_ID + " y must match the retail surface");
		assertEquals(RETAIL_Z, spot.getAttribute("z"), NPC_ID + " z must match the retail surface");
	}

	@Test
	void legacyShardgolemPositionIsGone() throws Exception {
		var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(SPAWNS.toFile());

		int legacySpots = ((Double) XPathFactory.newInstance().newXPath().evaluate(
			"count(/spawns/spawn_map[@map_id='301570000']/spawn[@npc_id='" + NPC_ID + "']/spot[@x='" + LEGACY_X + "'])",
			document, XPathConstants.NUMBER)).intValue();
		assertEquals(0, legacySpots, "legacy shardgolem position must not spawn a second copy");
	}
}
