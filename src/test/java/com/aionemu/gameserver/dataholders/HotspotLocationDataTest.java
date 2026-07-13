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
}
