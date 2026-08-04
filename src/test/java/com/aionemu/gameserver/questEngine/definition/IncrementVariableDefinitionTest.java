package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Increment-variable action advances the packed variable by delta. */
class IncrementVariableDefinitionTest {
	@Test
	void incrementVariableAdvancesFieldByDelta() {
		var dsl = quest(990031)
			.metadata(QuestMetadata.minimal("increment-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 2)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 4)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.then(incrementVariable("var0", 2)).goTo("done").compile();

		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, 990031, QuestStatus.START, 2, Map.of()),
			new QuestEvent.TalkToNpc(203057, 31, 0),
			dsl.definition().transitions().getFirst());

		assertTrue(plan.isPresent());
		assertEquals(4, plan.orElseThrow().nextPackedVariables());
	}
}
