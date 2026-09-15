package com.aionemu.gameserver.world.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * 校验免装箱世界查表的二分实现。
 * Verifies the boxing-free world lookup binary search.
 */
class RealGeoDataLookupTest {

	private static final int[] IDS = { 110010000, 210040000, 220010000, 310030000 };

	@Test
	void findsEveryPresentId() {
		for (int i = 0; i < IDS.length; i++) {
			assertEquals(i, RealGeoData.indexOfWorldId(IDS, IDS[i]));
		}
	}

	@Test
	void reportsMissingIds() {
		assertEquals(-1, RealGeoData.indexOfWorldId(IDS, 1000));
		assertEquals(-1, RealGeoData.indexOfWorldId(IDS, 210040001));
		assertEquals(-1, RealGeoData.indexOfWorldId(IDS, 210039999));
		assertEquals(-1, RealGeoData.indexOfWorldId(IDS, 999999999));
	}

	@Test
	void handlesEmptyAndSingleElementArrays() {
		assertEquals(-1, RealGeoData.indexOfWorldId(new int[0], 123));
		assertEquals(0, RealGeoData.indexOfWorldId(new int[] { 123 }, 123));
		assertEquals(-1, RealGeoData.indexOfWorldId(new int[] { 123 }, 124));
	}

	@Test
	void emptySnapshotHasNoMap() {
		assertNull(RealGeoData.WorldMapLookup.EMPTY.find(210040000));
	}
}
