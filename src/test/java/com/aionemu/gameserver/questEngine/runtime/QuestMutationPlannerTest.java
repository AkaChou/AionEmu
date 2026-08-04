package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeAllItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.worldNpcIs;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void zoneConditionUsesCapturedCurrentZoneFactsAndFailsClosedWhenUnknown() {
		CompiledQuestDefinition definition = QuestDsl.quest(QUEST_ID + 2)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.UseItem(182400002)).from("started")
			.when(QuestDsl.zoneIs("BERITRAS_WEAPON_220040000"))
			.goTo("reward")
			.compile();
		var transition = definition.definition().transitions().get(0);
		QuestSnapshot inside = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of("BERITRAS_WEAPON_220040000")));
		QuestSnapshot outside = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of("BELUSLAN_FORTRESS_220040000")));
		QuestSnapshot unknown = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of());

		assertTrue(QuestMutationPlanner.plan(definition, inside,
			new QuestEvent.UseItem(182400002), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, outside,
			new QuestEvent.UseItem(182400002), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, unknown,
			new QuestEvent.UseItem(182400002), transition).isPresent());
	}

	@Test
	void beautifulFeatherCleansEveryWorkItemOnRewardRoutes() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/2392.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}
		List<QuestAction> cleanup = List.of(
			new QuestAction.RemoveItem(182204159, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(182204160, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(182204161, QuestAction.RemoveItem.ALL));
		var routes = definition.definition().transitions().stream()
			.filter(transition -> Set.of("r1", "r2", "r3").contains(transition.sourceNode()))
			.filter(transition -> transition.targetNode().equals("complete")
				|| transition.targetNode().equals(transition.sourceNode()))
			.toList();

		assertTrue(routes.size() >= 6);
		assertTrue(routes.stream().allMatch(route -> route.actions().containsAll(cleanup)));
	}

	@Test
	void ringForLuckRemovesItsQuestWorkItemWhenCompleting() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/2578.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}

		var completion = definition.definition().transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();

		assertTrue(completion.actions().contains(
			new QuestAction.RemoveItem(182204453, QuestAction.RemoveItem.ALL)));
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
