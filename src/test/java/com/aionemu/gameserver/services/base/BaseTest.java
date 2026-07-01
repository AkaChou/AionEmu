package com.aionemu.gameserver.services.base;

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
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.base.BaseNpc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.world.World;

class BaseTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private GameWorldBootstrapServices worldBootstrapServices;

	@AfterEach
	void tearDown() {
		if (worldBootstrapServices != null) {
			worldBootstrapServices.destroy();
		}
	}

	@Test
	void stopToleratesBaseNpcRemovalFromWorldCollectionDuringDelete() {
		List<BaseNpc> liveNpcs = new ArrayList<BaseNpc>();
		TestBaseNpc firstNpc = baseNpc(liveNpcs);
		TestBaseNpc secondNpc = baseNpc(liveNpcs);
		TestBaseNpc thirdNpc = baseNpc(liveNpcs);
		liveNpcs.add(firstNpc);
		liveNpcs.add(secondNpc);
		liveNpcs.add(thirdNpc);
		TestWorld world = objenesis.newInstance(TestWorld.class);
		world.npcs = liveNpcs;
		worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));

		assertDoesNotThrow(() -> new Base<TestBaseLocation>(new TestBaseLocation()).stop());
		assertTrue(liveNpcs.isEmpty());
	}

	@Test
	void despawnAttackersToleratesAttackerRemovalDuringDelete() {
		Base<TestBaseLocation> base = new Base<TestBaseLocation>(new TestBaseLocation());
		List<Npc> attackers = base.getAttackers();
		TestBaseNpc firstNpc = attackerNpc(attackers);
		TestBaseNpc secondNpc = attackerNpc(attackers);
		TestBaseNpc thirdNpc = attackerNpc(attackers);
		attackers.add(firstNpc);
		attackers.add(secondNpc);
		attackers.add(thirdNpc);

		assertDoesNotThrow(base::despawnAttackers);
		assertTrue(attackers.isEmpty());
	}

	private TestBaseNpc baseNpc(Collection<BaseNpc> liveNpcs) {
		TestBaseNpc npc = objenesis.newInstance(TestBaseNpc.class);
		npc.controller = new RemovingNpcController(liveNpcs, npc);
		return npc;
	}

	private TestBaseNpc attackerNpc(Collection<Npc> liveNpcs) {
		TestBaseNpc npc = objenesis.newInstance(TestBaseNpc.class);
		npc.controller = new RemovingAttackerController(liveNpcs, npc);
		return npc;
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class TestBaseLocation extends BaseLocation {
		@Override
		public int getId() {
			return 101;
		}
	}

	private static final class TestWorld extends World {
		private Collection<BaseNpc> npcs;

		@Override
		public Collection<BaseNpc> getLocalBaseNpcs(int locationId) {
			return npcs;
		}
	}

	private static final class TestBaseNpc extends BaseNpc {
		private NpcController controller;

		private TestBaseNpc() {
			super(0, new NpcController(), null, (NpcTemplate) null);
		}

		@Override
		public NpcController getController() {
			return controller;
		}
	}

	private static final class RemovingNpcController extends NpcController {
		private final Collection<BaseNpc> liveNpcs;
		private final BaseNpc npc;

		private RemovingNpcController(Collection<BaseNpc> liveNpcs, BaseNpc npc) {
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
