package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeAllItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.worldNpcIs;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMutationPlannerTest {
	private static final int QUEST_ID = 1300;
	private static final int ITEM_ID = 182400001;

	@Test
	void removeAllUsesCapturedInventoryFactsWithoutComparingAgainstSentinel() {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0,
			Map.of(ITEM_ID, 4));

		assertTrue(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void removeAllFailsClosedWhenInventoryFactsAreUnknown() {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0,
			null, Map.of(), false, true, 0);

		assertFalse(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void worldNpcConditionUsesCapturedCurrentInstanceFactsAndFailsClosedWhenUnknown() {
		CompiledQuestDefinition definition = QuestDsl.quest(QUEST_ID + 1)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.TalkToNpc(700243)).from("started")
			.when(worldNpcIs(212814, false)).goTo("reward")
			.compile();
		QuestSnapshot absent = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(java.util.Set.of()));
		QuestSnapshot present = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(java.util.Set.of(212814)));
		QuestSnapshot unknown = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of());
		var transition = definition.definition().transitions().get(0);

		assertTrue(QuestMutationPlanner.plan(definition, absent,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, present,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, unknown,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
	}

	private static CompiledQuestDefinition definition() {
		return QuestDsl.quest(QUEST_ID)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.TalkToNpc(700001)).from("started")
			.then(removeAllItem(ITEM_ID)).goTo("reward")
			.compile();
	}
}
