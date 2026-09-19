package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 龙脊深渊任务副本双生守护者的真端出生面闸门。
 * Gate for the Drakenspire Depths quest-instance twin protector spawn surface.
 *
 * <p>背景：任务副本 301520000 曾经在真端出生面（531.088501 / 530.858398 的守护者之泉位置）之外，
 * 残留一对手写的旧出生点（545.58734 / 545.7349，来自非任务副本的 Lv2 形态布局），
 * 导致 237228（Lava Protector）与 237229（Heatvent Protector）各自刷出 2 个。
 * 真端 {@code IDSeal_Q/world_N.xml} 中 {@code IDSeal_Q_Twin_P_N_65_Ah} 与
 * {@code IDSeal_Q_Twin_M_N_65_Ah} 都只有 1 条 count=1 的出生记录，位于上述真端出生面。</p>
 */
class DrakenspireDepthsQTwinSpawnSurfaceTest {

	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml");

	/** npc_id、x、y、z（真端出生面）/ npc_id, x, y, z of the retail spawn surface. */
	private static final String[][] TWINS = {
		{"237228", "531.088501", "212.438065", "1683.411621"}, // Lava Protector.
		{"237229", "530.858398", "151.868103", "1683.411621"}  // Heatvent Protector.
	};

	/** 非真端残留坐标，必须整体消失（含 z 变体）/ Legacy positions that must be gone entirely. */
	private static final String[] LEGACY_POSITIONS = {"545.58734", "545.7349"};

	@Test
	void eachTwinSpawnsExactlyOnceAtTheRetailSurface() throws Exception {
		var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(SPAWNS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();

		for (String[] twin : TWINS) {
			String npcId = twin[0];
			NodeList spawns = (NodeList) xpath.evaluate(
				"/spawns/spawn_map[@map_id='301520000']/spawn[@npc_id='" + npcId + "']",
				document, XPathConstants.NODESET);
			assertEquals(1, spawns.getLength(), npcId + " must have exactly one spawn block");

			NodeList spots = ((Element) spawns.item(0)).getElementsByTagName("spot");
			assertEquals(1, spots.getLength(), npcId + " must have exactly one spawn point");

			Element spot = (Element) spots.item(0);
			assertEquals(twin[1], spot.getAttribute("x"), npcId + " x must match the retail surface");
			assertEquals(twin[2], spot.getAttribute("y"), npcId + " y must match the retail surface");
			assertEquals(twin[3], spot.getAttribute("z"), npcId + " z must match the retail surface");
		}
	}

	@Test
	void legacyHandWrittenTwinPositionsAreGone() throws Exception {
		String source = Files.readString(SPAWNS);
		for (String legacy : LEGACY_POSITIONS) {
			assertFalse(source.contains(legacy),
				"legacy twin position " + legacy + " must not spawn a second copy");
		}
	}
}
