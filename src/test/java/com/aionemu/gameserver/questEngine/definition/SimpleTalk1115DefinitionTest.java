package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setStatus;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vertical fixture for the true-server SimpleTalk entry 1115. */
class SimpleTalk1115DefinitionTest {
	@Test
	void trueServerSimpleTalkXmlAndDslCompileToTheSameDefinition() throws Exception {
		CompiledQuestDefinition fromXml;
		try (InputStream input = getClass().getResourceAsStream(
			"/quest-definition-fixtures/simpletalk-1115.xml")) {
			fromXml = QuestDefinitionXmlCompiler.compile(input);
		}

		CompiledQuestDefinition fromDsl = simpleTalk1115().compile();

		assertEquals(fromDsl.definition(), fromXml.definition());
		assertEquals(QuestOwnership.RETAIL_ALIGNED, fromXml.ownership());
		assertEquals(List.of("TALK_TO_NPC"), fromXml.transitionsByType().keySet().stream().toList());
	}

	@Test
	void talkChainProjectsOnlyTheProvenStatusAndVarTransitions() {
		CompiledQuestDefinition definition = simpleTalk1115().compile();

		var acquisition = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1115, QuestStatus.NONE, 0, Map.of()),
			talkToNpc(203075), definition.definition().transitions().get(0));
		assertTrue(acquisition.isPresent());
		assertEquals(QuestStatus.START, acquisition.orElseThrow().nextStatus());
		assertEquals(0, acquisition.orElseThrow().nextPackedVariables());

		var intermediate = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1115, QuestStatus.START, 0, Map.of()),
			talkToNpc(203072), definition.definition().transitions().get(1));
		assertTrue(intermediate.isPresent());
		assertEquals(QuestStatus.START, intermediate.orElseThrow().nextStatus());
		assertEquals(1, intermediate.orElseThrow().nextPackedVariables());

		var completion = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1115, QuestStatus.START, 1, Map.of()),
			talkToNpc(203058), definition.definition().transitions().get(2));
		assertTrue(completion.isPresent());
		assertEquals(QuestStatus.REWARD, completion.orElseThrow().nextStatus());
		assertEquals(1, completion.orElseThrow().nextPackedVariables());
	}

	private static QuestDsl.QuestBuilder simpleTalk1115() {
		QuestMetadata metadata = new QuestMetadata("The Elim's Message", 1102215, 4, Integer.MAX_VALUE,
			Set.of("ELYOS"), "QUEST", RepeatPolicy.once(), Set.of(), List.of(),
			List.of(new QuestReward("GOLD", 0, 680), new QuestReward("EXP", 0, 2673)), List.of(), Set.of(), "", 0,
			1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0,
			List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
		QuestDsl.QuestBuilder builder = quest(1115)
			.metadata(metadata)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("feira-spoken", project(QuestStatus.START, vars("var0", 1)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)));
		builder.on(talkToNpc(203075)).from("unaccepted").goTo("started");
		builder.on(talkToNpc(203072)).from("started").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 0)).then(setVariable("var0", 1)).goTo("feira-spoken");
		builder.on(talkToNpc(203058)).from("feira-spoken").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 1)).then(setStatus(QuestStatus.REWARD)).goTo("reward");
		return builder;
	}
}
