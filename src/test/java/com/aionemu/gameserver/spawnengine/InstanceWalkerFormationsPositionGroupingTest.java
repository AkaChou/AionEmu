package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;

/**
 * 验证实例巡逻队的近邻坐标分组。
 * Verifies proximity-based position grouping for instance walker formations.
 */
class InstanceWalkerFormationsPositionGroupingTest {

	private static final Path DREADGION_SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/302200000_Dredgion_Defense_Sanctum.xml");
	private static final Path GELKMAROS_SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Npcs/220070000_Gelkmaros.xml");
	private static final List<String> DREADGION_ROUTES = List.of(
		"idlc1_dreadgion_npcpathmain2",
		"idlc1_dreadgion_npcpathbase_mob_18",
		"idlc1_dreadgion_npcpathbase_mob_16",
		"idlc1_dreadgion_npcpathmain14",
		"idlc1_dreadgion_npcpathmain7",
		"idlc1_dreadgion_npcpathmain15",
		"idlc1_dreadgion_npcpathmain11",
		"idlc1_dreadgion_npcpathidlc1_dreadgion_main_5.5",
		"idlc1_dreadgion_npcpathbase_mob_9",
		"idlc1_dreadgion_NPCPathMain8",
		"idlc1_dreadgion_npcpathmain19",
		"idlc1_dreadgion_npcpathbase_mob_26");

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void groupsDreadgionSurfaceAdjustedSpawnsIntoCompleteFormations() throws Exception {
		for (String routeId : DREADGION_ROUTES) {
			List<List<ClusteredNpc>> groups = groupsFromSpawnFile(DREADGION_SPAWNS, routeId);
			assertEquals(1, groups.size(), routeId);
			assertEquals(3, groups.getFirst().size(), routeId);
		}
	}

	@Test
	void groupsPoolSizedGelkmarosRouteAcrossRetailAnchorSpread() throws Exception {
		List<List<ClusteredNpc>> groups = groupsFromSpawnFile(GELKMAROS_SPAWNS,
			"6E070A628CDFB97DE9C54EA88B9A1C7D5FC5FBE4");

		assertEquals(1, groups.size());
		assertEquals(3, groups.getFirst().size());
	}

	@Test
	void groupsNearbyPairWithinFormationSpacing() {
		List<ClusteredNpc> candidates = List.of(
			candidate(0, 0, 3),
			candidate(1.8f, 0, 3));

		List<List<ClusteredNpc>> groups = InstanceWalkerFormations.groupCandidates(candidates);

		assertEquals(1, groups.size());
		assertEquals(2, groups.getFirst().size());
	}

	@Test
	void keepsGroupsSeparatedBeyondFormationSpacing() {
		List<ClusteredNpc> candidates = List.of(
			candidate(0, 0, 3),
			candidate(0, 0, 3),
			candidate(0, 0, 3),
			candidate(5, 0, 3),
			candidate(5, 0, 3),
			candidate(5, 0, 3));

		List<List<ClusteredNpc>> groups = InstanceWalkerFormations.groupCandidates(candidates);

		assertEquals(2, groups.size());
		assertEquals(3, groups.get(0).size());
		assertEquals(3, groups.get(1).size());
	}

	private List<List<ClusteredNpc>> groupsFromSpawnFile(Path path, String routeId) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var document = factory.newDocumentBuilder().parse(path.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList spots = (NodeList) xpath.evaluate(
			"/spawns/spawn_map/spawn/spot[@walker_id='" + routeId + "']",
			document, XPathConstants.NODESET);
		assertEquals(3, spots.getLength(), routeId);

		List<ClusteredNpc> candidates = new ArrayList<>(spots.getLength());
		for (int i = 0; i < spots.getLength(); i++) {
			Element spot = (Element) spots.item(i);
			candidates.add(candidate(Float.parseFloat(spot.getAttribute("x")),
				Float.parseFloat(spot.getAttribute("y")), 3));
		}
		return InstanceWalkerFormations.groupCandidates(candidates);
	}

	private ClusteredNpc candidate(float x, float y, int poolSize) {
		TestNpc npc = objenesis.newInstance(TestNpc.class);
		npc.setSpawn(SpawnEngine.createSpawnTemplate(0, 0, x, y, 0, (byte) 0));
		WalkerTemplate template = new WalkerTemplate("test-route");
		template.setPool(poolSize);
		return new ClusteredNpc(npc, 0, template);
	}

	private static final class TestNpc extends Npc {

		private TestNpc() {
			super(0, new NpcController(), null, null);
		}
	}
}
