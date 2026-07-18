package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamRoute;
import org.junit.jupiter.api.Test;

class WindstreamDefinitionLoaderTest {

	@Test
	void loadsRetailWindstreamsWithCanonicalAndCompatibilityMapIds() {
		WindstreamData data = WindstreamDefinitionLoader.load(
			new File("src/main/resources/aion/definitions/compact/world/fly_path.xml"),
			new File("src/main/resources/aion/definitions/compact/wind.xml"),
			new File("src/main/resources/aion/definitions/compact/id-mappings.xml"));

		assertEquals(27, data.size());
		assertNotNull(data.getRoute(210130000, 376001));
		WindstreamRoute questRoute = data.getRoute(210130000, 405001);
		assertEquals(405, questRoute.getId());
		assertTrue(questRoute.contains(0, 1407, 366, 590, 45));
		assertFalse(questRoute.contains(0, 1500, 366, 590, 45));
		WindstreamRoute kasparOutbound = data.getRoute(210130000, 390001);
		assertTrue(kasparOutbound.contains(600, 2582, 334, 529, 45));
		WindstreamRoute kasparReturn = data.getRoute(210130000, 389001);
		assertFalse(kasparReturn.contains(500, 2450.58f, 332.34f, 501.94f, 45));
		WindstreamRoute kasparHill = data.getRoute(210130000, 407001);
		assertTrue(kasparHill.contains(9500, 2514.431f, 333.30823f, 485.11697f, 45));
		assertNull(data.getRoute(210130000, 77001));
		assertNotNull(data.getRoute(600041100, 218001));
		assertNotNull(data.getRoute(600040000, 218001));
		assertNotNull(data.getRoute(600051000, 241001));
		assertNotNull(data.getRoute(600050000, 241001));
		assertEquals(0, data.getStreamTemplate(210100000).getLocations().getLocation().stream()
			.filter(location -> location.getId() == 302).findFirst().orElseThrow().getState());
	}

	@Test
	void validatesMovementAgainstInterpolatedRoutePosition() {
		WindstreamRoute route = new WindstreamRoute(1, 2, 1000,
			List.of(new Point3D(0, 0, 0), new Point3D(10, 0, 0)));

		assertEquals(5, route.positionAt(500).getX());
		assertTrue(route.contains(500, 5, 0, 0, 1));
		assertFalse(route.contains(500, 7, 0, 0, 1));
		assertFalse(route.contains(1001, 10, 0, 0, 1));
	}
}
