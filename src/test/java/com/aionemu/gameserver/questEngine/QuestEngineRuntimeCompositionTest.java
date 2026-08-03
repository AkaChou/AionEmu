package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestOwnership;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestSpawnRegistry;
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
			.ownership(QuestOwnership.RETAIL_ALIGNED)
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
	void unsupportedProductionEventFailsBeforeRegisteringAnyNpcOwner() {
		QuestEngine engine = new QuestEngine();
		var talk = QuestDsl.quest(990001)
			.ownership(QuestOwnership.RETAIL_ALIGNED)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start").goTo("start")
			.compile();
		var attack = QuestDsl.quest(990002)
			.ownership(QuestOwnership.RETAIL_ALIGNED)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.AttackNpc(210133)).from("start").goTo("start")
			.compile();

		assertThrows(IllegalStateException.class,
			() -> engine.installProductionDefinitions(new ImmutableQuestCatalog(java.util.List.of(talk, attack))));

		assertEquals(java.util.List.of(), engine.getQuestNpc(203057).getOnTalkEvent());
		assertFalse(engine.isHaveHandler(990001));
	}
}
