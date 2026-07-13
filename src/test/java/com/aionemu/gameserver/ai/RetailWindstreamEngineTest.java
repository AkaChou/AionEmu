package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.model.flypath.FlyPathType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.windstreams.Location2D;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailWindstreamEngineTest {

	@Test
	void keepsWindstreamStateIsolatedPerInstance() {
		WindstreamData previous = DataManager.WINDSTREAM_DATA;
		Location2D location = new Location2D(159, 1, FlyPathType.ONE_WAY);
		WorldMapInstance first = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		WorldMapInstance second = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		try {
			DataManager.WINDSTREAM_DATA = new WindstreamData(
				List.of(new WindstreamTemplate(300250000, List.of(location))), List.of());

			assertTrue(RetailWindstreamEngine.supports(300250000, 159));
			assertFalse(RetailWindstreamEngine.supports(300250000, 999));
			assertTrue(RetailWindstreamEngine.setEnabled(first, 159, false));
			assertEquals(0, RetailWindstreamEngine.state(first, location));
			assertEquals(1, RetailWindstreamEngine.state(second, location));

			RetailWindstreamEngine.clear(first);
			assertEquals(1, RetailWindstreamEngine.state(first, location));
		} finally {
			RetailWindstreamEngine.clear(first);
			RetailWindstreamEngine.clear(second);
			DataManager.WINDSTREAM_DATA = previous;
		}
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return 300250000;
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
