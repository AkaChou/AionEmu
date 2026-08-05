package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestEngineEscortAndProximityRegistrationTest {
	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	@Test
	void productionAcceptsEscortRecoveryAndProximityEvents() {
		QuestEngine engine = new QuestEngine();
		engine.installProductionDefinitions(new ImmutableQuestCatalog(List.of(
			definition(990007, new QuestEvent.AtDistance(835303)),
			definition(990008, new QuestEvent.NpcReachTarget()),
			definition(990009, new QuestEvent.NpcLostTarget()),
			definition(990010, new QuestEvent.LogOut()))));

		assertEquals(List.of(990007), engine.getQuestNpc(835303).getOnDistanceEvent());
	}

	private static com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition definition(
		int id, QuestEvent event) {
		return QuestDsl.quest(id)
			.progress(bitField("var0", 0, 2, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(event).from("started").goTo("reward")
			.compile();
	}
}
