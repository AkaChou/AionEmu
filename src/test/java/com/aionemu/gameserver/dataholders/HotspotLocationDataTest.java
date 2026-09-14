package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.teleport.HotspotlocationTemplate;

class HotspotLocationDataTest {

	@Test
	void loadsCompactHotspotLocations() {
		HotspotLocationData data = HotspotLocationData.load(
			new File("src/main/resources/aion/definitions/compact/hotspot_location/hotspot_location.xml"));

		assertEquals(107, data.size());
		HotspotlocationTemplate template = data.getHotspotlocationTemplate(15);
		assertNotNull(template);
		assertEquals(210010000, template.getMapId());
		assertEquals(427.0f, template.getX());
		assertEquals(1741.0f, template.getY());
		assertEquals(120.0f, template.getZ());
		assertEquals(45, template.getHeading());
		assertEquals(44, template.getPrice());
		assertNotNull(data.getHotspotlocationTemplate(42));
	}

	@Test
	void balaureaHotspotsTargetLiveWorldsInsteadOfMasterMirrors() {
		HotspotLocationData data = HotspotLocationData.load(
			new File("src/main/resources/aion/definitions/compact/hotspot_location/hotspot_location.xml"));

		assertHotspotWorld(data, 210050000, 72, 73, 74, 116, 118);
		assertHotspotWorld(data, 220070000, 75, 76, 77, 117, 121);
	}

	private static void assertHotspotWorld(HotspotLocationData data, int worldId, int... locIds) {
		for (int locId : locIds) {
			HotspotlocationTemplate template = data.getHotspotlocationTemplate(locId);
			assertNotNull(template, "hotspot " + locId);
			assertEquals(worldId, template.getMapId(), "hotspot " + locId);
		}
	}
}
