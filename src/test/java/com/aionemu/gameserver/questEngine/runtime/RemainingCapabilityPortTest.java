package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestAiPerceptionFacts;
import com.aionemu.gameserver.questEngine.definition.QuestHousingFacts;
import com.aionemu.gameserver.questEngine.definition.QuestMovementFacts;
import com.aionemu.gameserver.questEngine.definition.QuestPvpInstanceFacts;
import com.aionemu.gameserver.questEngine.definition.QuestRecoveryFacts;
import com.aionemu.gameserver.questEngine.definition.QuestSkillFacts;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Every newly composed event port rejects an incomplete authoritative boundary. */
class RemainingCapabilityPortTest {
	@AfterEach
	void cleanup() {
		QuestSpawnRegistry.global().cleanupAll();
	}

	@Test
	void aiHousingMovementInstanceAndSkillPortsFailClosedWithoutPlayerContext() {
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestAiPerceptionEventPort().addAggroList(null, 277224));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestHousingEventPort().houseItemUse(null, 3420021));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestMovementEventPort().passFlyingRing(null, "RING"));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestPvpInstanceEventPort().dredgionReward(null));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestSkillEventPort().useSkill(null, 9832));
	}

	@Test
	void recoveryCleanupIsSafeToRepeatAndLogoutFactsRemainRequired() {
		PlayerQuestRecoveryEventPort port = new PlayerQuestRecoveryEventPort();
		assertThrows(IllegalArgumentException.class, () -> port.logOut(null));
		assertDoesNotThrow(() -> port.recover(null));
	}

	@Test
	void runtimeFactsUseTheSameEventIndexRouteAsDefinitions() {
		CompiledQuestDefinition definition = QuestDsl.quest(14211)
			.progress(QuestDsl.bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", QuestDsl.project(QuestStatus.START, QuestDsl.vars("var0", 0)))
			.node("done", QuestDsl.project(QuestStatus.REWARD, QuestDsl.vars("var0", 1)))
			.on(QuestDsl.addAggroList(277224)).from("start").when(QuestDsl.statusIs(QuestStatus.START))
			.goTo("done").compile();
		QuestAiPerceptionFacts facts = new QuestAiPerceptionFacts(7, 20, 277224, 20, true, true,
			210130000, 210130000, 1, 1, 10d, 50, true, true);
		assertEquals(List.of(14211), new QuestEventIndex(new ImmutableQuestCatalog(List.of(definition)))
			.routesFor(new QuestEvent.AddAggroList(277224, facts)).stream()
			.map(QuestEventIndex.Route::questId).toList());
	}

	@Test
	void capturesAuthoritativeFactsForAllRemainingEventPorts() throws Exception {
		Player player = player(7, 210130000, 1, 1f, 2f, 3f);
		Npc npc = npc(20, 277224, 210130000, 1, 4f, 6f, 3f);
		Player aggroSource = player(30, 210130000, 1, 4f, 6f, 3f);
		QuestEnv npcEnv = new QuestEnv(npc, player, 0, 0);

		QuestEvent.AddAggroList ai = new PlayerQuestAiPerceptionEventPort(
			(source, target) -> true, (target, recipient) -> 5d, ignored -> 50)
			.addAggroList(npcEnv, 277224, aggroSource);
		assertEquals(7, ai.facts().recipientId());
		assertEquals(30, ai.facts().aggroSourceObjectId());
		assertEquals(5d, ai.facts().distance());

		House house = house(40, 7, 1001, 520010000, 1);
		QuestEvent.HouseItemUse housing = new PlayerQuestHousingEventPort(ignored -> house)
			.houseItemUse(new QuestEnv(null, player, 0, 0), 3420021, 9001);
		QuestHousingFacts housingFacts = housing.facts();
		assertEquals(40, housingFacts.houseObjectId());
		assertEquals(3420021, housingFacts.itemTemplateId());
		assertEquals(9001, housingFacts.itemObjectId());

		PlayerQuestMovementEventPort movementPort = new PlayerQuestMovementEventPort();
		QuestMovementFacts ringFacts = movementPort.passFlyingRing(
			new QuestEnv(null, player, 0, 0), "ERACUS_TEMPLE_AIR_BOOSTER_1").facts();
		assertEquals(210130000, ringFacts.worldId());
		assertEquals(1, ringFacts.instanceId());
		assertEquals("ERACUS_TEMPLE_AIR_BOOSTER_1", ringFacts.actionId());
		assertEquals("405001", movementPort.enterWindStream(
			new QuestEnv(null, player, 0, 0), 405001).facts().actionId());

		PlayerQuestPvpInstanceEventPort instancePort = new PlayerQuestPvpInstanceEventPort();
		QuestPvpInstanceFacts instanceFacts = instancePort.dredgionReward(
			new QuestEnv(null, player, 0, 0)).facts();
		assertEquals("DREDGION", instanceFacts.instanceKind());
		assertFalse(instanceFacts.statisticsCaptured());
		assertEquals("KAMAR", instancePort.kamarReward(new QuestEnv(null, player, 0, 0)).facts().instanceKind());
		assertEquals("OPHIDAN", instancePort.ophidanReward(new QuestEnv(null, player, 0, 0)).facts().instanceKind());
		assertEquals("BASTION", instancePort.bastionReward(new QuestEnv(null, player, 0, 0)).facts().instanceKind());

		QuestSkillFacts skillFacts = new PlayerQuestSkillEventPort().useSkill(npcEnv, 9832).facts();
		assertEquals(7, skillFacts.casterId());
		assertEquals(20, skillFacts.targetObjectId());
		assertEquals(277224, skillFacts.targetTemplateId());

		QuestRecoveryFacts recoveryFacts = new PlayerQuestRecoveryEventPort()
			.logOut(new QuestEnv(null, player, 0, 0)).facts();
		assertEquals(7, recoveryFacts.playerId());
		assertEquals(1, recoveryFacts.instanceId());
	}

	@Test
	void portsRejectNonAuthoritativeRoutesAndRecoveryCleansPlayerResources() throws Exception {
		Player player = player(7, 210130000, 1, 0f, 0f, 0f);
		Npc npc = npc(20, 277224, 210130000, 1, 3f, 4f, 0f);
		Player source = player(30, 210130000, 1, 3f, 4f, 0f);
		QuestEnv npcEnv = new QuestEnv(npc, player, 0, 0);

		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestAiPerceptionEventPort((left, right) -> false,
				(left, right) -> 5d, ignored -> 50).addAggroList(npcEnv, 277224, source));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestAiPerceptionEventPort((left, right) -> true,
				(left, right) -> 51d, ignored -> 50).addAggroList(npcEnv, 277224, source));

		House wrongOwner = house(40, 8, 1001, 520010000, 1);
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerQuestHousingEventPort(ignored -> wrongOwner)
				.houseItemUse(new QuestEnv(null, player, 0, 0), 3420021, 9001));

		Player unavailable = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, unavailable, "objectId", 8);
		assertThrows(IllegalStateException.class,
			() -> new PlayerQuestMovementEventPort().passFlyingRing(
				new QuestEnv(null, unavailable, 0, 0), "RING"));
		assertThrows(IllegalStateException.class,
			() -> new PlayerQuestPvpInstanceEventPort().dredgionReward(
				new QuestEnv(null, unavailable, 0, 0)));
		assertThrows(IllegalStateException.class,
			() -> new PlayerQuestSkillEventPort().useSkill(
				new QuestEnv(null, unavailable, 0, 0), 9832));

		QuestSnapshot snapshot = new QuestSnapshot(7, 2333, QuestStatus.START, 0, Map.of(), Map.of());
		Npc registered = new ObjenesisStd().newInstance(Npc.class);
		registered.setPosition(new WorldPosition(210130000));
		assertEquals(true, QuestSpawnRegistry.global().register(snapshot, "escort", registered));
		new PlayerQuestRecoveryEventPort().recover(new QuestEnv(null, player, 0, 0));
		assertFalse(QuestSpawnRegistry.global().contains(snapshot, "escort"));
	}

	private static Player player(int objectId, int worldId, int instanceId,
			float x, float y, float z) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", objectId);
		player.setPosition(position(worldId, instanceId, x, y, z));
		return player;
	}

	private static Npc npc(int objectId, int templateId, int worldId, int instanceId,
			float x, float y, float z) throws Exception {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(AionObject.class, npc, "objectId", objectId);
		NpcTemplate template = new ObjenesisStd().newInstance(NpcTemplate.class);
		setField(NpcTemplate.class, template, "npcId", templateId);
		setField(VisibleObject.class, npc, "objectTemplate", template);
		npc.setPosition(position(worldId, instanceId, x, y, z));
		return npc;
	}

	private static House house(int objectId, int ownerId, int addressId,
			int worldId, int instanceId) throws Exception {
		House house = new ObjenesisStd().newInstance(House.class);
		setField(AionObject.class, house, "objectId", objectId);
		setField(House.class, house, "playerObjectId", ownerId);
		HouseAddress address = new ObjenesisStd().newInstance(HouseAddress.class);
		setField(HouseAddress.class, address, "id", addressId);
		setField(House.class, house, "address", address);
		house.setPosition(position(worldId, instanceId, 1f, 2f, 3f));
		return house;
	}

	private static WorldPosition position(int worldId, int instanceId,
			float x, float y, float z) {
		TestWorldPosition position = new TestWorldPosition(worldId, instanceId);
		position.setXYZH(x, y, z, (byte) 0);
		return position;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestWorldPosition extends WorldPosition {
		private final int instanceId;

		private TestWorldPosition(int worldId, int instanceId) {
			super(worldId);
			this.instanceId = instanceId;
		}

		@Override
		public int getInstanceId() {
			return instanceId;
		}

		@Override
		public boolean isSpawned() {
			return true;
		}
	}
}
