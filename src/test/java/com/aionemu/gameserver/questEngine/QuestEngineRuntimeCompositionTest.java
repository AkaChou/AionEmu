package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestSpawnRegistry;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestProximityEventPort;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestEventPort;
import com.aionemu.gameserver.questEngine.runtime.TypedQuestAfterCommitPort;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEngineRuntimeCompositionTest {
	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void cleanup() {
		QuestSpawnRegistry.global().cleanupAll();
		DataManager.NPC_DATA = originalNpcData;
	}

	@Test
	void engineMountsTheTypedProductionPorts() {
		QuestRuntimeComposition composition = new QuestEngine().runtimeComposition();

		assertInstanceOf(TypedQuestAfterCommitPort.class, composition.afterCommitPort());
		assertInstanceOf(PlayerQuestEventPort.class, composition.eventPort());
		assertTrue(composition.actionPort() != null);
		assertTrue(composition.statePort() != null);
		assertInstanceOf(PlayerQuestProximityEventPort.class, composition.proximityEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestAiPerceptionEventPort.class,
			composition.aiPerceptionEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestHousingEventPort.class,
			composition.housingEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestMovementEventPort.class,
			composition.movementEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestPvpInstanceEventPort.class,
			composition.pvpInstanceEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestSkillEventPort.class,
			composition.skillEventPort());
		assertInstanceOf(com.aionemu.gameserver.questEngine.runtime.PlayerQuestRecoveryEventPort.class,
			composition.recoveryEventPort());
	}

	@Test
	void engineClearReleasesQuestOwnedRuntimeResources() {
		QuestSnapshot snapshot = new QuestSnapshot(7, 2333, QuestStatus.START, 0, Map.of(), Map.of());
		QuestSpawnRegistry registry = QuestSpawnRegistry.global();
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		npc.setPosition(new WorldPosition(310040000));
		assertTrue(registry.register(snapshot, "escort", npc));

		new QuestEngine().clear();

		assertFalse(registry.contains(snapshot, "escort"));
	}

	@Test
	void talkAndKillProductionEventsRegisterNpcIndexes() {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990001)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("one-kill", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("unaccepted").goTo("started")
			.on(new QuestEvent.KillNpc(210133)).from("started").goTo("one-kill")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		assertEquals(java.util.List.of(990001), engine.getQuestNpc(203057).getOnTalkEvent());
		assertEquals(java.util.List.of(990001), engine.getQuestNpc(203057).getOnQuestStart());
		assertEquals(java.util.List.of(990001), engine.getQuestNpc(210133).getOnKillEvent());
		assertTrue(engine.isHaveHandler(990001));
	}

	@Test
	void itemPlayProductionEventIsAcceptedByTheCentralInstallationGate() {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990003)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.ItemPlay(182201728, 3000)).from("start").goTo("started")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		assertTrue(engine.isHaveHandler(990003));
	}

	@Test
	void getItemProductionEventIsAcceptedByTheCentralInstallationGate() {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990004)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.GetItem(182216178)).from("started").goTo("reward")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		assertTrue(engine.isHaveHandler(990004));
	}

	@Test
	void equipItemProductionEventIsAcceptedByTheCentralInstallationGate() throws ReflectiveOperationException {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990014)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("equipped", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.EquipItem(140000003)).from("started").goTo("equipped")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		var equipListeners = QuestEngine.class.getDeclaredField("questOnEquipItem");
		equipListeners.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Integer, java.util.Set<Integer>> registrations =
			(Map<Integer, java.util.Set<Integer>>) equipListeners.get(engine);
		assertEquals(java.util.Set.of(990014), registrations.get(140000003));
		assertTrue(engine.isHaveHandler(990014));
	}

	@Test
	void dieProductionEventIsAcceptedByTheCentralInstallationGate() throws ReflectiveOperationException {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990005)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("recovered", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.Die()).from("started").goTo("recovered")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		var dieListeners = QuestEngine.class.getDeclaredField("questOnDie");
		dieListeners.setAccessible(true);
		assertTrue(((IntArrayList) dieListeners.get(engine)).contains(990005));
		assertTrue(engine.isHaveHandler(990005));
	}

	@Test
	void attackProductionEventRegistersNpcRouteAndPassesTheInstallationGate() {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990006)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.AttackNpc(210319)).from("started").goTo("reward")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		assertEquals(java.util.List.of(990006), engine.getQuestNpc(210319).getOnAttackEvent());
		assertTrue(engine.isHaveHandler(990006));
	}

	@Test
	void movementProductionEventsPassTheInstallationGate() {
		QuestEngine engine = new QuestEngine();
		var ring = QuestDsl.quest(990011)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.PassFlyingRing("TEST_RING")).from("started").goTo("reward")
			.compile();
		var wind = QuestDsl.quest(990012)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.EnterWindStream(405001)).from("started").goTo("reward")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(ring, wind)));

		assertTrue(engine.isHaveHandler(990011));
		assertTrue(engine.isHaveHandler(990012));
	}

	@Test
	void remainingCatalogEventsPassTheInstallationGateAndBuildRoutes() throws ReflectiveOperationException {
		QuestEngine engine = new QuestEngine();
		var definition = QuestDsl.quest(990015)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.on(new QuestEvent.Abandon()).from("started").goTo("unaccepted")
			.on(new QuestEvent.DredgionReward()).from("started").goTo("started")
			.on(new QuestEvent.HouseItemUse(3420021)).from("started").goTo("started")
			.on(new QuestEvent.KillInWorld(210010000)).from("started").goTo("started")
			.on(new QuestEvent.KillRanked(4)).from("started").goTo("started")
			.on(new QuestEvent.LeaveZone("TEST_ZONE")).from("started").goTo("started")
			.on(new QuestEvent.QuestTimerEnd()).from("started").goTo("started")
			.on(new QuestEvent.UseSkill(9832)).from("started").goTo("started")
			.compile();

		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(definition)));

		var dispatcherField = QuestEngine.class.getDeclaredField("productionDispatcher");
		dispatcherField.setAccessible(true);
		QuestProductionDispatcher dispatcher = (QuestProductionDispatcher) dispatcherField.get(engine);
		assertTrue(dispatcher.hasRoutes(new QuestEvent.Abandon()));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.DredgionReward()));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.HouseItemUse(3420021)));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.KillInWorld(210010000)));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.KillRanked(12)));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.LeaveZone("TEST_ZONE")));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.QuestTimerEnd()));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.UseSkill(9832)));
		assertTrue(engine.isProductionOwner(990015));
	}

	@Test
	void preparedProductionCatalogIsNotPublishedUntilInstallation() {
		QuestEngine engine = new QuestEngine();
		var oldDefinition = reloadDefinition(990020, "old snapshot");
		var newDefinition = reloadDefinition(990021, "new snapshot");
		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(oldDefinition)));

		QuestEngine.PreparedProductionDefinitions prepared = engine.prepareProductionDefinitions(
			new ImmutableQuestCatalog(java.util.List.of(newDefinition)));

		assertTrue(engine.questCatalog().findExecutable(990020).isPresent());
		assertFalse(engine.questCatalog().findExecutable(990021).isPresent());
		assertEquals("new snapshot", prepared.catalog().findMetadata(990021).orElseThrow().name());
		assertTrue(prepared.dispatcher().owns(990021));
	}

	@Test
	void unsupportedProductionEventKeepsThePreviouslyPublishedSnapshot() {
		QuestEngine engine = new QuestEngine();
		var previous = reloadDefinition(990020, "old snapshot");
		engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(previous)));
		var talk = QuestDsl.quest(990001)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start").goTo("start")
			.compile();
		var unsupported = QuestDsl.quest(990002)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.KamarReward()).from("start").goTo("start")
			.compile();

		assertThrows(IllegalStateException.class,
			() -> engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(talk, unsupported))));

		assertEquals(java.util.List.of(), engine.getQuestNpc(203057).getOnTalkEvent());
		assertFalse(engine.isHaveHandler(990001));
		assertTrue(engine.isProductionOwner(990020));
		assertEquals("old snapshot", engine.questCatalog().findMetadata(990020).orElseThrow().name());
	}

	private static com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition reloadDefinition(
			int questId, String name) {
		return QuestDsl.quest(questId)
			.metadata(QuestMetadata.minimal(name, questId, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.LevelUp()).from("start").goTo("start")
			.compile();
	}
}
