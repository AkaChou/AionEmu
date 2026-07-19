package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.DynamicArea;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailDynamicAreaEngineTest {

	private static final int WORLD_ID = 300250000;
	private static final String JUMP = "MOVING_COLLISION_JUMP";
	private static final String WINDBOX = "MOVING_COLLISION_WINDBOX";

	@Test
	void enablesAreaDuringNormalDailyWindow() {
		DynamicArea area = area(JUMP, 8, 18, false);

		assertFalse(RetailDynamicAreaEngine.initialState(area, 7));
		assertTrue(RetailDynamicAreaEngine.initialState(area, 8));
		assertTrue(RetailDynamicAreaEngine.initialState(area, 17));
		assertFalse(RetailDynamicAreaEngine.initialState(area, 18));
	}

	@Test
	void enablesAreaDuringWindowAcrossMidnight() {
		DynamicArea area = area(JUMP, 22, 8, false);

		assertFalse(RetailDynamicAreaEngine.initialState(area, 21));
		assertTrue(RetailDynamicAreaEngine.initialState(area, 22));
		assertTrue(RetailDynamicAreaEngine.initialState(area, 0));
		assertTrue(RetailDynamicAreaEngine.initialState(area, 7));
		assertFalse(RetailDynamicAreaEngine.initialState(area, 8));
	}

	@Test
	void usesAlwaysEnabledWhenScheduleHasNoWindow() {
		assertTrue(RetailDynamicAreaEngine.initialState(area(JUMP, 0, 0, true), 12));
		assertFalse(RetailDynamicAreaEngine.initialState(area(JUMP, 0, 0, false), 12));
	}

	@Test
	void mapsMovingCollisionTypesToProtocolTypes() {
		assertEquals(0, RetailDynamicAreaEngine.packetType(WINDBOX));
		assertEquals(2, RetailDynamicAreaEngine.packetType(JUMP));
	}

	@Test
	void keepsStateIsolatedPerInstanceAndClearsIt() {
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		DynamicArea area = area(JUMP, 0, 0, true);
		WorldMapInstance first = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		WorldMapInstance second = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		try {
			DataManager.RETAIL_AI_DATA = data(area);

			assertTrue(RetailDynamicAreaEngine.setEnabled(first, JUMP, area.id(), false));
			assertFalse(RetailDynamicAreaEngine.state(first, area, 12));
			assertTrue(RetailDynamicAreaEngine.state(second, area, 12));

			RetailDynamicAreaEngine.clear(first);
			assertTrue(RetailDynamicAreaEngine.state(first, area, 12));
		} finally {
			RetailDynamicAreaEngine.clear(first);
			RetailDynamicAreaEngine.clear(second);
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void restoresPersistedAreaStateInAnotherRuntimeInstance() {
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		DynamicArea area = area(JUMP, 0, 0, true);
		WorldMapInstance first = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		WorldMapInstance restored = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		try {
			DataManager.RETAIL_AI_DATA = data(area);
			assertTrue(RetailDynamicAreaEngine.setEnabled(first, JUMP, area.id(), false));
			restored.setDynamicInstance(dynamic(), InstanceRuntimeState.decode(first.getRuntimeState().encode()));

			assertFalse(RetailDynamicAreaEngine.state(restored, area, 12));
		} finally {
			RetailDynamicAreaEngine.clear(first);
			RetailDynamicAreaEngine.clear(restored);
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	private static DynamicInstance dynamic() {
		return new DynamicInstance(1, WORLD_ID, 1, 1, 1, DynamicInstance.OWNER_MATCH, 1, (byte) 0,
			DynamicInstance.ACTIVE, (byte) 0, 0, 0, 0, 0, 1, "", 0);
	}

	private static DynamicArea area(String type, int startTime, int endTime, boolean alwaysEnabled) {
		return new DynamicArea(WORLD_ID, "test", type, 7, "test", false, startTime, endTime, 0, alwaysEnabled);
	}

	private static RetailAiData data(DynamicArea area) {
		return new RetailAiData(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
			Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
			Map.of(WORLD_ID, Map.of(area.type(), Map.of(area.id(), area))));
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return WORLD_ID;
		}

		@Override
		public void doOnAllPlayers(Visitor<Player> visitor) {
		}

		@Override
		public MapRegion getRegion(float x, float y, float z) {
			return null;
		}

		@Override
		protected MapRegion createMapRegion(int regionId) {
			return null;
		}

		@Override
		protected void initMapRegions() {
		}

		@Override
		public boolean isPersonal() {
			return false;
		}

		@Override
		public int getOwnerId() {
			return 0;
		}
	}
}
