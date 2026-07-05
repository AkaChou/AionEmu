package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.AbyssNpcType;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;

class PlayerAggroLevelTest {

	private static final int TEST_INSTANCE_MAP = 300000001;

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private int originalAggroLevelImmune;
	private List<Integer> originalInstanceAggro;

	@BeforeEach
	void setUp() throws Exception {
		originalAggroLevelImmune = AIConfig.AGGRO_LEVEL_IMMUNE;
		originalInstanceAggro = new ArrayList<>(instanceAggro());
		instanceAggro().clear();
		AIConfig.AGGRO_LEVEL_IMMUNE = 10;
	}

	@AfterEach
	void tearDown() throws Exception {
		AIConfig.AGGRO_LEVEL_IMMUNE = originalAggroLevelImmune;
		instanceAggro().clear();
		instanceAggro().addAll(originalInstanceAggro);
	}

	@Test
	void normalMonsterInAggroInstanceDoesNotBypassLevelImmunity() throws Exception {
		instanceAggro().add(TEST_INSTANCE_MAP);

		TestPlayer player = player(66);
		TestNpc npc = npc(50);
		npc.inInstance = true;
		npc.worldId = TEST_INSTANCE_MAP;

		assertFalse(player.isAggroFrom(npc));
	}

	@Test
	void normalMonsterStillAggrosBelowTheImmuneLevelGap() {
		TestPlayer player = player(59);
		TestNpc npc = npc(50);

		assertTrue(player.isAggroFrom(npc));
	}

	@Test
	void guardAggroStillBypassesLevelImmunity() {
		TestPlayer player = player(66);
		TestNpc npc = npc(50);
		npc.tribe = TribeClass.ATKGUARD_LIGHT;

		assertTrue(player.isAggroFrom(npc));
	}

	private TestPlayer player(int level) {
		TestPlayer player = objenesis.newInstance(TestPlayer.class);
		player.level = (byte) level;
		return player;
	}

	private TestNpc npc(int level) {
		TestNpc npc = objenesis.newInstance(TestNpc.class);
		npc.level = (byte) level;
		npc.tribe = TribeClass.AGGRESSIVEMONSTER;
		npc.template = new TestNpcTemplate();
		return npc;
	}

	@SuppressWarnings("unchecked")
	private static List<Integer> instanceAggro() throws Exception {
		Field field = InstanceService.class.getDeclaredField("instanceAggro");
		field.setAccessible(true);
		return (List<Integer>) field.get(null);
	}

	private static final class TestPlayer extends Player {
		private byte level;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public byte getLevel() {
			return level;
		}

		@Override
		public boolean isAggroIconTo(Npc npc) {
			return true;
		}
	}

	private static final class TestNpc extends Npc {
		private byte level;
		private boolean inInstance;
		private int worldId;
		private TribeClass tribe;
		private TestNpcTemplate template;

		private TestNpc() {
			super(0, null, null, null);
		}

		@Override
		public byte getLevel() {
			return level;
		}

		@Override
		public boolean isInInstance() {
			return inInstance;
		}

		@Override
		public int getWorldId() {
			return worldId;
		}

		@Override
		public TribeClass getTribe() {
			return tribe;
		}

		@Override
		public NpcTemplate getObjectTemplate() {
			return template;
		}
	}

	private static final class TestNpcTemplate extends NpcTemplate {
		@Override
		public AbyssNpcType getAbyssNpcType() {
			return AbyssNpcType.NONE;
		}
	}
}
