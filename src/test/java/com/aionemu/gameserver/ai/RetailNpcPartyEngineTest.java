package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.RetailAiData.NpcParty;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcPartyMember;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RetailNpcPartyEngineTest {

	@Test
	void createsEveryExplicitMemberWithWorldScopedStableToken() {
		List<SpawnTemplate> templates = RetailNpcPartyEngine.createSpawnTemplates(300540000, List.of(
			new NpcParty("world/world_N.xml#party4", List.of(
					new NpcPartyMember(231113, 10, 20, 30, 10, true),
					new NpcPartyMember(231111, 12, 22, 32, 30)))));

		assertEquals(2, templates.size());
		assertEquals(List.of(231113, 231111), templates.stream().map(SpawnTemplate::getNpcId).toList());
		assertEquals(List.of("300540000:world/world_N.xml#party4", "300540000:world/world_N.xml#party4"),
			templates.stream().map(SpawnTemplate::getNpcPartyId).toList());
		assertEquals(List.of(10f, 12f), templates.stream().map(SpawnTemplate::getX).toList());
		assertEquals(List.of(20f, 22f), templates.stream().map(SpawnTemplate::getY).toList());
		assertEquals(List.of(30f, 32f), templates.stream().map(SpawnTemplate::getZ).toList());
		assertEquals(List.of((byte) 10, (byte) 30), templates.stream().map(SpawnTemplate::getHeading).toList());
		assertEquals(List.of(true, true), templates.stream().map(SpawnTemplate::isResolveZ).toList());
		assertEquals(List.of(1, 0), templates.stream().map(SpawnTemplate::getFly).toList());
	}

	@Test
	void constructedNpcsDiscoverPartyMembersInTheSameInstance() throws ReflectiveOperationException {
		List<SpawnTemplate> templates = RetailNpcPartyEngine.createSpawnTemplates(300540000, List.of(
			new NpcParty("world/world_N.xml#party4", List.of(
					new NpcPartyMember(231113, 10, 20, 30, 10),
					new NpcPartyMember(231111, 12, 22, 32, 30)))));
		NpcSkillData oldNpcSkillData = DataManager.NPC_SKILL_DATA;
		DataManager.NPC_SKILL_DATA = new NpcSkillData();
		try {
			AI2Engine.getInstance().registerAI(DummyAI2.class);
			Npc first = new Npc(1, new NpcController(), templates.get(0), npcTemplate());
			Npc second = new Npc(2, new NpcController(), templates.get(1), npcTemplate());
			TestWorldMapInstance instance = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
			instance.npcs = List.of(first, second);
			MapRegion region = region(instance);
			spawnAt(first, region);
			spawnAt(second, region);

			assertEquals(templates.get(0).getNpcPartyId(), first.getNpcPartyId());
			assertEquals(templates.get(1).getNpcPartyId(), second.getNpcPartyId());
			assertSame(second, RetailNpcParty.members(first).getFirst());
		} finally {
			DataManager.NPC_SKILL_DATA = oldNpcSkillData;
		}
	}

	private static NpcTemplate npcTemplate() throws ReflectiveOperationException {
		NpcStatsTemplate stats = new NpcStatsTemplate();
		stats.setMaxHp(1);
		stats.setMaxMp(1);
		NpcTemplate template = new NpcTemplate();
		setField(NpcTemplate.class, template, "ai", "dummy");
		template.setStatsTemplate(stats);
		return template;
	}

	private static void spawnAt(Npc npc, MapRegion region) throws ReflectiveOperationException {
		WorldPosition position = npc.getPosition();
		setField(WorldPosition.class, position, "mapRegion", region);
		setField(WorldPosition.class, position, "isSpawned", true);
	}

	private static MapRegion region(WorldMapInstance instance) throws ReflectiveOperationException {
		Constructor<MapRegion> constructor = MapRegion.class
			.getDeclaredConstructor(int.class, WorldMapInstance.class, ZoneInstance[].class);
		constructor.setAccessible(true);
		return constructor.newInstance(0, instance, new ZoneInstance[0]);
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private List<Npc> npcs;

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public List<Npc> getNpcs() {
			return npcs;
		}

		@Override
		public int getInstanceId() {
			return 7;
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
