package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.world.World;

class SiegeServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private GameWorldBootstrapServices worldBootstrapServices;

	@AfterEach
	void tearDown() {
		if (worldBootstrapServices != null) {
			worldBootstrapServices.destroy();
		}
	}

	@Test
	void activeSiegeIndexIsSafeForCronAndThreadPoolThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(SiegeService.class.getDeclaredField("activeSieges").getType()));
	}

	@Test
	void deSpawnNpcsToleratesNpcRemovalFromWorldCollectionDuringDelete() {
		List<SiegeNpc> liveNpcs = new ArrayList<SiegeNpc>();
		TestSiegeNpc firstNpc = siegeNpc(liveNpcs);
		TestSiegeNpc secondNpc = siegeNpc(liveNpcs);
		TestSiegeNpc thirdNpc = siegeNpc(liveNpcs);
		liveNpcs.add(firstNpc);
		liveNpcs.add(secondNpc);
		liveNpcs.add(thirdNpc);
		TestWorld world = objenesis.newInstance(TestWorld.class);
		world.npcs = liveNpcs;
		worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));

		assertDoesNotThrow(() -> new SiegeService().deSpawnNpcs(1011));
		assertTrue(liveNpcs.isEmpty());
	}

	private TestSiegeNpc siegeNpc(Collection<SiegeNpc> liveNpcs) {
		TestSiegeNpc npc = objenesis.newInstance(TestSiegeNpc.class);
		npc.controller = new RemovingNpcController(liveNpcs, npc);
		return npc;
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class TestWorld extends World {
		private Collection<SiegeNpc> npcs;

		@Override
		public Collection<SiegeNpc> getLocalSiegeNpcs(int locationId) {
			return npcs;
		}
	}

	private static final class TestSiegeNpc extends SiegeNpc {
		private NpcController controller;

		private TestSiegeNpc() {
			super(0, new NpcController(), null, (NpcTemplate) null);
		}

		@Override
		public NpcController getController() {
			return controller;
		}
	}

	private static final class RemovingNpcController extends NpcController {
		private final Collection<SiegeNpc> liveNpcs;
		private final SiegeNpc npc;

		private RemovingNpcController(Collection<SiegeNpc> liveNpcs, SiegeNpc npc) {
			this.liveNpcs = liveNpcs;
			this.npc = npc;
		}

		@Override
		public void onDelete() {
			liveNpcs.remove(npc);
		}
	}
}
