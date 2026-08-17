package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class CMObjectSearchTest {

	private static final int NPC_ID = 730007;
	private static final int WORLD_ID = 210010000;
	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void skipsDeadAndDisappearedTargetsBeforeSelectingNextLivingLocation() {
		SpawnSearchResult deadLocation = location(10);
		SpawnSearchResult disappearedLocation = location(20);
		SpawnSearchResult livingLocation = location(30);
		Npc livingNpc = npc(30, true, false);

		CM_OBJECT_SEARCH.SearchTarget result = CM_OBJECT_SEARCH.selectSearchTarget(NPC_ID,
				List.of(deadLocation, disappearedLocation, livingLocation),
				List.of(npc(10, true, true), npc(20, false, false), livingNpc), false);

		assertSame(livingLocation, result.location());
		assertSame(livingNpc, result.npc());
	}

	@Test
	void onlyGmSearchMayFallBackToHardToFindStaticLocation() {
		SpawnSearchResult noahLocation = location(558.419983f);

		assertNull(CM_OBJECT_SEARCH.selectSearchTarget(NPC_ID, List.of(noahLocation), List.of(), false));
		CM_OBJECT_SEARCH.SearchTarget result = CM_OBJECT_SEARCH.selectSearchTarget(NPC_ID,
				List.of(noahLocation), List.of(), true);
		assertSame(noahLocation, result.location());
		assertNull(result.npc());
	}

	@Test
	void gmCanUseAVisibleTargetWithoutAStaticSearchLocation() {
		Npc livingNpc = npc(40, true, false);

		CM_OBJECT_SEARCH.SearchTarget result = CM_OBJECT_SEARCH.selectSearchTarget(NPC_ID, List.of(),
				List.of(livingNpc), true);

		assertNull(result.location());
		assertSame(livingNpc, result.npc());
	}

	@Test
	void resolvesTheQuestAcestesAliasAtTheFirstAndReportStages() {
		assertEquals(802051, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652,
			questState(14047, QuestStatus.START, 3)));
		assertEquals(802051, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652,
			questState(14047, QuestStatus.START, 6)));
	}

	@Test
	void keepsTheLegacyAcestesIdOutsideTheQuestStages() {
		assertEquals(204652, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652,
			questState(14047, QuestStatus.START, 4)));
		assertEquals(204652, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652,
			questState(2696, QuestStatus.START, 0)));
		assertEquals(204652, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652,
			questState(14047, QuestStatus.COMPLETE, 3)));
		assertEquals(204652, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(204652, null));
		assertEquals(802051, CM_OBJECT_SEARCH.resolveQuestSearchNpcId(802051,
			questState(14047, QuestStatus.START, 3)));
	}

	private static QuestState questState(int questId, QuestStatus status, int questVar0) {
		return new QuestState(questId, status, questVar0, 0, null, null, null);
	}

	private static SpawnSearchResult location(float x) {
		SpawnSpotTemplate spot = new SpawnSpotTemplate();
		setField(SpawnSpotTemplate.class, spot, "x", x);
		setField(SpawnSpotTemplate.class, spot, "y", 1375.589966f);
		setField(SpawnSpotTemplate.class, spot, "z", 119.999374f);
		return new SpawnSearchResult(WORLD_ID, spot);
	}

	private static StubNpc npc(float x, boolean spawned, boolean dead) {
		StubNpc npc = OBJENESIS.newInstance(StubNpc.class);
		npc.npcId = NPC_ID;
		npc.worldId = WORLD_ID;
		npc.spawned = spawned;
		npc.spawn = OBJENESIS.newInstance(SpawnTemplate.class);
		npc.spawn.setX(x);
		npc.spawn.setY(1375.589966f);
		npc.spawn.setZ(119.999374f);
		npc.lifeStats = OBJENESIS.newInstance(StubNpcLifeStats.class);
		npc.lifeStats.dead = dead;
		return npc;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class StubNpc extends Npc {
		private int npcId;
		private int worldId;
		private boolean spawned;
		private SpawnTemplate spawn;
		private StubNpcLifeStats lifeStats;

		private StubNpc() {
			super(0, (NpcController) null, (SpawnTemplate) null, (NpcTemplate) null);
		}

		@Override
		public int getNpcId() {
			return npcId;
		}

		@Override
		public int getWorldId() {
			return worldId;
		}

		@Override
		public boolean isSpawned() {
			return spawned;
		}

		@Override
		public SpawnTemplate getSpawn() {
			return spawn;
		}

		@Override
		public NpcLifeStats getLifeStats() {
			return lifeStats;
		}
	}

	private static final class StubNpcLifeStats extends NpcLifeStats {
		private boolean dead;

		private StubNpcLifeStats() {
			super(null);
		}

		@Override
		public boolean isAlreadyDead() {
			return dead;
		}
	}
}
