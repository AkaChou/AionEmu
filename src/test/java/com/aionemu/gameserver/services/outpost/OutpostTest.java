package com.aionemu.gameserver.services.outpost;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.outpost.OutpostNpc;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.world.World;

class OutpostTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private GameWorldBootstrapServices worldBootstrapServices;

	@AfterEach
	void tearDown() {
		if (worldBootstrapServices != null) {
			worldBootstrapServices.destroy();
		}
	}

	@Test
	void stopToleratesOutpostNpcRemovalFromWorldCollectionDuringDelete() {
		List<OutpostNpc> liveNpcs = new ArrayList<OutpostNpc>();
		TestOutpostNpc firstNpc = outpostNpc(liveNpcs);
		TestOutpostNpc secondNpc = outpostNpc(liveNpcs);
		TestOutpostNpc thirdNpc = outpostNpc(liveNpcs);
		liveNpcs.add(firstNpc);
		liveNpcs.add(secondNpc);
		liveNpcs.add(thirdNpc);
		TestWorld world = objenesis.newInstance(TestWorld.class);
		world.npcs = liveNpcs;
		worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));

		assertDoesNotThrow(() -> new Outpost<TestOutpostLocation>(new TestOutpostLocation()).stop());
		assertTrue(liveNpcs.isEmpty());
	}

	@Test
	void despawnAttackersToleratesAttackerRemovalDuringDelete() {
		Outpost<TestOutpostLocation> outpost = new Outpost<TestOutpostLocation>(new TestOutpostLocation());
		List<Npc> attackers = outpost.getAttackers();
		TestOutpostNpc firstNpc = attackerNpc(attackers);
		TestOutpostNpc secondNpc = attackerNpc(attackers);
		TestOutpostNpc thirdNpc = attackerNpc(attackers);
		attackers.add(firstNpc);
		attackers.add(secondNpc);
		attackers.add(thirdNpc);

		assertDoesNotThrow(outpost::despawnAttackers);
		assertTrue(attackers.isEmpty());
	}

	private TestOutpostNpc outpostNpc(Collection<OutpostNpc> liveNpcs) {
		TestOutpostNpc npc = objenesis.newInstance(TestOutpostNpc.class);
		npc.controller = new RemovingNpcController(liveNpcs, npc);
		return npc;
	}

	private TestOutpostNpc attackerNpc(Collection<Npc> liveNpcs) {
		TestOutpostNpc npc = objenesis.newInstance(TestOutpostNpc.class);
		npc.controller = new RemovingAttackerController(liveNpcs, npc);
		return npc;
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class TestOutpostLocation extends OutpostLocation {
		@Override
		public int getId() {
			return 101;
		}
	}

	private static final class TestWorld extends World {
		private Collection<OutpostNpc> npcs;

		@Override
		public Collection<OutpostNpc> getLocalOutpostNpcs(int locationId) {
			return npcs;
		}
	}

	private static final class TestOutpostNpc extends OutpostNpc {
		private NpcController controller;

		private TestOutpostNpc() {
			super(0, new NpcController(), null, (NpcTemplate) null);
		}

		@Override
		public NpcController getController() {
			return controller;
		}
	}

	private static final class RemovingNpcController extends NpcController {
		private final Collection<OutpostNpc> liveNpcs;
		private final OutpostNpc npc;

		private RemovingNpcController(Collection<OutpostNpc> liveNpcs, OutpostNpc npc) {
			this.liveNpcs = liveNpcs;
			this.npc = npc;
		}

		@Override
		public void onDelete() {
			liveNpcs.remove(npc);
		}
	}

	private static final class RemovingAttackerController extends NpcController {
		private final Collection<Npc> liveNpcs;
		private final Npc npc;

		private RemovingAttackerController(Collection<Npc> liveNpcs, Npc npc) {
			this.liveNpcs = liveNpcs;
			this.npc = npc;
		}

		@Override
		public void onDelete() {
			liveNpcs.remove(npc);
		}
	}
}
