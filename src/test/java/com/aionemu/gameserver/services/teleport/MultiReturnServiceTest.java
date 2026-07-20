package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PortalLocData;
import com.aionemu.gameserver.model.Race;

class MultiReturnServiceTest {
	@Test
	void mapsMergedBalaureaWorldsToExistingPortalLocations() {
		assertEquals(2101300, MultiReturnService.getTeleportWorldId(210130000, Race.ELYOS));
		assertEquals(2201400, MultiReturnService.getTeleportWorldId(220140000, Race.ASMODIANS));
	}

	@Test
	void missingPortalLocationDoesNotLeaveTheInstance() {
		PortalLocData previous = DataManager.PORTAL_LOC_DATA;
		DataManager.PORTAL_LOC_DATA = new PortalLocData();
		try {
			assertDoesNotThrow(() -> MultiReturnService.Teleport(null, 0, 0));
		} finally {
			DataManager.PORTAL_LOC_DATA = previous;
		}
	}

	@Test
	void staticTeleportReferencesResolveToUniquePortalLocations() throws Exception {
		Path data = Path.of("src/main/resources/aion/data/static_data");
		var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		NodeList worlds = builder.parse(data.resolve("world_maps.xml").toFile()).getElementsByTagName("map");
		Set<Integer> worldIds = new HashSet<>();
		for (int i = 0; i < worlds.getLength(); i++) {
			worldIds.add(Integer.parseInt(((Element) worlds.item(i)).getAttribute("id")));
		}

		NodeList locations = builder.parse(data.resolve("portals/portal_loc.xml").toFile()).getElementsByTagName("portal_loc");
		Map<Integer, Integer> locationWorlds = new HashMap<>();
		for (int i = 0; i < locations.getLength(); i++) {
			Element location = (Element) locations.item(i);
			int id = Integer.parseInt(location.getAttribute("loc_id"));
			int worldId = Integer.parseInt(location.getAttribute("world_id"));
			assertTrue(locationWorlds.put(id, worldId) == null, () -> "Duplicate portal loc " + id);
		}

		NodeList paths = builder.parse(data.resolve("portals/portal_template2.xml").toFile()).getElementsByTagName("portal_path");
		for (int i = 0; i < paths.getLength(); i++) {
			int id = Integer.parseInt(((Element) paths.item(i)).getAttribute("loc_id"));
			assertTrue(locationWorlds.containsKey(id), () -> "Missing portal loc " + id);
			assertTrue(worldIds.contains(locationWorlds.get(id)), () -> "Unavailable portal world for loc " + id);
		}

		NodeList returns = builder.parse(data.resolve("items/multi_returns.xml").toFile()).getElementsByTagName("loc");
		for (int i = 0; i < returns.getLength(); i++) {
			Element destination = (Element) returns.item(i);
			int worldId = Integer.parseInt(destination.getAttribute("world_id"));
			int itemId = Integer.parseInt(((Element) destination.getParentNode()).getAttribute("id"));
			int locationId = MultiReturnService.getTeleportWorldId(worldId, itemId % 2 == 0 ? Race.ELYOS : Race.ASMODIANS);
			assertTrue(locationWorlds.containsKey(locationId), () -> "Missing multi-return portal loc " + locationId);
			assertEquals(worldId, locationWorlds.get(locationId), () -> "Wrong multi-return world for loc " + locationId);
		}
	}
}
