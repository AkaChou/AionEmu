package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class WaterVolumeStoreTest {

	@Test
	void loadsGeneratedClientWaterVolumes() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		try (InputStream input = getClass().getResourceAsStream("/aion/geo/water-volumes.bin")) {
			assertNotNull(input);
			assertEquals(773, store.load(input));
		}

		assertNotNull(store.find(600050000, 749.36853f, 2320.6953f, 90));
	}

	@Test
	void findsOnlyTheNearestWaterSurfaceContainingThePoint() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		store.load(data(
				new float[][] {{0, 0, 5}, {10, 0, 5}, {10, 10, 5}, {0, 10, 5}},
				new float[][] {{0, 0, 15}, {10, 0, 15}, {10, 10, 15}, {0, 10, 15}}));

		WaterVolumeStore.Volume lower = store.find(1, 5, 5, 4);
		WaterVolumeStore.Volume upper = store.find(1, 5, 5, 10);

		assertNotNull(lower);
		assertEquals(1, lower.id());
		assertNotNull(upper);
		assertEquals(2, upper.id());
		assertNull(store.find(1, 20, 5, 4));
	}

	@Test
	void interpolatesSlopedWaterSurface() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		store.load(data(new float[][] {{0, 0, 0}, {10, 0, 10}, {0, 10, 0}}));

		WaterVolumeStore.Volume volume = store.find(1, 2, 2, 1);

		assertNotNull(volume);
		assertEquals(2, volume.surfaceZ(2, 2), 0.001f);
	}

	@Test
	void triangulatesConcaveSlopedWaterSurface() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		store.load(data(new float[][] {{0, 0, 0}, {4, 0, 0}, {4, 4, 0}, {2, 2, 10}, {0, 4, 0}}));

		WaterVolumeStore.Volume volume = store.find(1, 3, 2.5f, 0);

		assertNotNull(volume);
		assertEquals(5, volume.surfaceZ(3, 2.5f), 0.001f);
	}

	@Test
	void rejectsSegmentThatCutsAcrossAConcaveShoreline() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		store.load(data(new float[][] {
				{0, 0, 5}, {4, 0, 5}, {4, 1, 5}, {1, 1, 5}, {1, 3, 5}, {4, 3, 5}, {4, 4, 5}, {0, 4, 5}}));
		WaterVolumeStore.Volume volume = store.find(1, 3.5f, 0.5f, 2);

		assertNotNull(volume);
		assertFalse(volume.allowsSegment(3.5f, 0.5f, 2, 3.5f, 3.5f, 2, 0.5f));
	}

	@Test
	void rejectsSegmentThatBrieflyLeavesWaterBetweenHeightSamples() throws Exception {
		WaterVolumeStore store = new WaterVolumeStore();
		store.load(data(new float[][] {
				{0, 0, 5}, {2, 0, 5}, {2, 2, 5}, {0.4f, 2, 5}, {0.4f, 0.9f, 5}, {0.3f, 0.9f, 5}, {0.3f, 2, 5}, {0, 2, 5}}));
		WaterVolumeStore.Volume volume = store.find(1, 0.1f, 1, 2);

		assertNotNull(volume);
		assertFalse(volume.allowsSegment(0.1f, 1, 2, 1.1f, 1, 2, 0.5f));
	}

	private static ByteArrayInputStream data(float[][]... volumes) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			output.writeInt(0x41495756);
			output.writeInt(1);
			output.writeInt(volumes.length);
			for (int i = 0; i < volumes.length; i++) {
				output.writeInt(1);
				output.writeInt(i + 1);
				output.writeInt(volumes[i].length);
				for (float[] point : volumes[i]) {
					output.writeFloat(point[0]);
					output.writeFloat(point[1]);
					output.writeFloat(point[2]);
				}
			}
		}
		return new ByteArrayInputStream(bytes.toByteArray());
	}
}
