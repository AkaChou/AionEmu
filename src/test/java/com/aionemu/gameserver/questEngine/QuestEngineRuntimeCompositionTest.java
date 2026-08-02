package com.aionemu.gameserver.questEngine;

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
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEngineRuntimeCompositionTest {
	@AfterEach
	void cleanup() {
		QuestSpawnRegistry.global().cleanupAll();
	}

	@Test
	void engineMountsTheFullyTypedProductionCompositionWithoutSwitchingOwners() {
		QuestRuntimeComposition composition = new QuestEngine().runtimeComposition();

		assertEquals(Set.of(
			QuestRuntimeComposition.BridgeClosure.AI_PERCEPTION,
			QuestRuntimeComposition.BridgeClosure.MOVIE,
			QuestRuntimeComposition.BridgeClosure.TIME,
			QuestRuntimeComposition.BridgeClosure.ESCORT_AI,
			QuestRuntimeComposition.BridgeClosure.HOUSING,
			QuestRuntimeComposition.BridgeClosure.MOVEMENT,
			QuestRuntimeComposition.BridgeClosure.CRAFT,
			QuestRuntimeComposition.BridgeClosure.PVP,
			QuestRuntimeComposition.BridgeClosure.PVP_INSTANCE,
			QuestRuntimeComposition.BridgeClosure.RECOVERY,
			QuestRuntimeComposition.BridgeClosure.SKILL), composition.bridgeClosures());
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
}
