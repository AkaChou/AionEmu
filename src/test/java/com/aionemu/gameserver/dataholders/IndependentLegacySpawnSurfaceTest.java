package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 真端变体块叠加 legacy 块的同族出生面闸门。
 * Gate for legacy blocks that were left behind when retail variant blocks were added.
 *
 * <p>这些 NPC 同时存在初始导入的 legacy 块和真端 {@code initial_delay} 变体块，
 * 加载器会把两条块都实例化，导致重复刷出。真端出生数据只包含本测试列出的坐标，
 * 因此每条记录必须只剩一个全 {@code resolve_z="true"} 的真端块。</p>
 */
class IndependentLegacySpawnSurfaceTest {

	private static final Path INSTANCES = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances");

	private static final List<ExpectedSpawn> EXPECTED = List.of(
		new ExpectedSpawn("300540000_Eternal_Bastion.xml", 230753, Set.of(
			"604.756287,884.442383,196.381195")),
		new ExpectedSpawn("300540000_Eternal_Bastion.xml", 230756, Set.of(
			"573.739319,508.922729,220.227386",
			"396.114319,276.650085,255.900757")),
		new ExpectedSpawn("300540000_Eternal_Bastion.xml", 233312, Set.of(
			"678.180359,765.124634,187.015808",
			"512.673828,309.350922,244.110962",
			"531.631287,259.061127,236.358643",
			"430.770569,313.178131,244.506516")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233474, Set.of(
			"506.807892,524.690613,603.298218",
			"483.596069,545.146729,603.298218")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233479, Set.of(
			"538.276123,430.987152,627.538513",
			"536.288330,449.273041,627.538513")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233480, Set.of(
			"528.553101,439.085052,627.538513")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233481, Set.of(
			"591.354919,559.109253,595.802917",
			"617.736145,551.529297,595.802917")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233484, Set.of(
			"676.447327,454.645477,606.976318",
			"684.240234,486.720276,606.976318")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233485, Set.of(
			"684.050476,457.937805,606.976318",
			"689.660034,481.769653,606.976318")),
		new ExpectedSpawn("301210000_Engulfed_Ophidan_Bridge.xml", 233486, Set.of(
			"682.244263,478.472900,606.976318",
			"678.488159,462.819489,606.976318")),
		new ExpectedSpawn("301390000_Drakenspire_Depths.xml", 236126, Set.of(
			"413.249481,249.259033,1684.473755",
			"413.655884,113.729584,1684.473755",
			"404.307648,228.275543,1684.473755",
			"404.308014,136.197388,1684.473755")),
		new ExpectedSpawn("301390000_Drakenspire_Depths.xml", 236223, Set.of(
			"762.575378,250.841385,1702.402100")),
		new ExpectedSpawn("301400000_The_Shugo_Emperor_Vault.xml", 235653, Set.of(
			"553.786499,458.800354,400.235992",
			"528.897217,491.745880,400.235992",
			"541.132507,467.304138,400.235992",
			"536.042480,479.770813,400.235992")),
		new ExpectedSpawn("301400000_The_Shugo_Emperor_Vault.xml", 235660, Set.of(
			"550.456421,393.938049,404.152008")));

	@Test
	void eachNpcKeepsOnlyTheRetailVariantSurface() throws Exception {
		Map<Path, Document> documents = new HashMap<>();
		for (ExpectedSpawn expected : EXPECTED) {
			Path file = INSTANCES.resolve(expected.file());
			Document document = documents.computeIfAbsent(file, IndependentLegacySpawnSurfaceTest::parse);
			List<Element> spawns = spawns(document, expected.npcId());
			assertEquals(1, spawns.size(), expected.npcId() + " must have exactly one spawn block");

			Set<String> actual = spots(spawns.get(0));
			assertEquals(expected.spots(), actual, expected.npcId() + " must keep only the retail surface");
		}
	}

	private static List<Element> spawns(Document document, int npcId) {
		NodeList nodes = document.getElementsByTagName("spawn");
		List<Element> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Element spawn = (Element) nodes.item(i);
			if (Integer.toString(npcId).equals(spawn.getAttribute("npc_id"))) {
				result.add(spawn);
			}
		}
		return result;
	}

	private static Set<String> spots(Element spawn) {
		NodeList nodes = spawn.getElementsByTagName("spot");
		Set<String> result = new HashSet<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Element spot = (Element) nodes.item(i);
			assertEquals("true", spot.getAttribute("resolve_z"), "spot must be a retail surface");
			result.add(spot.getAttribute("x") + "," + spot.getAttribute("y") + "," + spot.getAttribute("z"));
		}
		return result;
	}

	private static Document parse(Path path) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(path.toFile());
		} catch (Exception e) {
			throw new IllegalStateException("Cannot parse " + path, e);
		}
	}

	private record ExpectedSpawn(String file, int npcId, Set<String> spots) {}
}
