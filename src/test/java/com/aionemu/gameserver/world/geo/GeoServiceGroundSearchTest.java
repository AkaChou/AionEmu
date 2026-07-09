package com.aionemu.gameserver.world.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GeoServiceGroundSearchTest {

	private final boolean oldGeoEnable = GeoDataConfig.GEO_ENABLE;

	@AfterEach
	void tearDown() {
		GeoDataConfig.GEO_ENABLE = oldGeoEnable;
	}

	@Test
	void groundLookupDropsNpcBackToTerrainWhenCurrentZIsAlreadyTooHigh() throws Exception {
		GeoDataConfig.GEO_ENABLE = true;
		GeoService geoService = new GeoService();
		GeoMap map = new GeoMap("1001", 256);
		map.setTerrainData(new short[] {
			3200, 3200, 3200, 3200,
			3200, 3200, 3200, 3200,
			3200, 3200, 3200, 3200,
			3200, 3200, 3200, 3200
		}, 4, 4);
		setGeoData(geoService, new GeoData() {
			@Override
			public void loadGeoMaps() {
			}

			@Override
			public GeoMap getMap(int worldId) {
				return map;
			}
		});

		assertEquals(100.001F, geoService.getZ(1001, 2.5F, 2.5F, 150F, 0F, 1), 0.01F);
	}

	private static void setGeoData(GeoService geoService, GeoData geoData) throws Exception {
		Field field = GeoService.class.getDeclaredField("geoData");
		field.setAccessible(true);
		field.set(geoService, geoData);
	}
}
