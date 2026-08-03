package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.collectItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.killNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.itemPlay;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setStatus;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.useItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vertical compiler fixtures for the remaining true-server Simple* families. */
class SimpleQuestFamilyDefinitionTest {
	@Test
	void simpleSerialHunt9622XmlAndDslMatchTheFirstOrderedTarget() throws Exception {
		CompiledQuestDefinition definition = assertEquivalent("simpleserialhunt-9622.xml", simpleSerialHunt9622().compile());
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 9622, QuestStatus.START, 0, Map.of()),
			killNpc(216626), definition.definition().transitions().get(1));
		assertTrue(plan.isPresent());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
	}

	@Test
	void simpleCollect1103XmlAndDslMatchTheProvenActionItemEvent() throws Exception {
		CompiledQuestDefinition definition = assertEquivalent("simplecollect-1103.xml", simpleCollect1103().compile());
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1103, QuestStatus.START, 0, Map.of()),
			collectItem(700105, 1), definition.definition().transitions().get(1));
		assertTrue(plan.isPresent());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
	}

	@Test
	void simpleUseItem1107XmlAndDslMatchTheResolvedQuestItemAndRewardNpc() throws Exception {
		CompiledQuestDefinition definition = assertEquivalent("simpleuseitem-1107.xml", simpleUseItem1107().compile());
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1107, QuestStatus.NONE, 0, Map.of()),
			useItem(182200501), definition.definition().transitions().get(0));
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.START, plan.orElseThrow().nextStatus());
	}

	@Test
	void simpleItemPlay9623XmlAndDslKeepAnimationDurationExplicitlyUnresolved() throws Exception {
		CompiledQuestDefinition definition = assertEquivalent("simpleitemplay-9623.xml", simpleItemPlay9623().compile());
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 9623, QuestStatus.START, 2, Map.of()),
			itemPlay(182214024, 0), definition.definition().transitions().get(2));
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.REWARD, plan.orElseThrow().nextStatus());
	}

	private static CompiledQuestDefinition assertEquivalent(String resource, CompiledQuestDefinition fromDsl)
			throws Exception {
		CompiledQuestDefinition fromXml;
		try (InputStream input = SimpleQuestFamilyDefinitionTest.class.getResourceAsStream(
			"/quest-definition-fixtures/" + resource)) {
			fromXml = QuestDefinitionXmlCompiler.compile(input);
		}
		assertEquals(fromDsl.definition(), fromXml.definition());
		return fromXml;
	}

	private static QuestDsl.QuestBuilder simpleSerialHunt9622() {
		QuestDsl.QuestBuilder builder = base(9622, "SimpleSerialHunt 9622", "QUEST")
			.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("first-target", project(QuestStatus.START, vars("var0", 1)));
		builder.on(talkToNpc(203223)).from("unaccepted").goTo("started");
		builder.on(killNpc(216626)).from("started").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 0)).then(setVariable("var0", 1)).goTo("first-target");
		return builder;
	}

	private static QuestDsl.QuestBuilder simpleCollect1103() {
		QuestDsl.QuestBuilder builder = base(1103, "SimpleCollect 1103", "IMPORTANT")
			.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("object-collected", project(QuestStatus.START, vars("var0", 1)));
		builder.on(talkToNpc(203057)).from("unaccepted").goTo("started");
		builder.on(collectItem(700105, 1)).from("started").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 0)).then(setVariable("var0", 1)).goTo("object-collected");
		return builder;
	}

	private static QuestDsl.QuestBuilder simpleUseItem1107() {
		QuestDsl.QuestBuilder builder = base(1107, "SimpleUseItem 1107", "QUEST")
			.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("item-used", project(QuestStatus.START, vars("var0", 1)))
			.node("reward-npc", project(QuestStatus.REWARD, vars("var0", 1)));
		builder.on(useItem(182200501)).from("unaccepted").then(setVariable("var0", 1)).goTo("item-used");
		builder.on(talkToNpc(203075)).from("item-used").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 1)).then(setStatus(QuestStatus.REWARD)).goTo("reward-npc");
		return builder;
	}

	private static QuestDsl.QuestBuilder simpleItemPlay9623() {
		QuestDsl.QuestBuilder builder = base(9623, "SimpleItemPlay 9623", "QUEST")
			.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("first-dialog", project(QuestStatus.START, vars("var0", 1)))
			.node("second-dialog", project(QuestStatus.START, vars("var0", 2)))
			.node("item-played", project(QuestStatus.REWARD, vars("var0", 2)));
		builder.on(talkToNpc(203223)).from("unaccepted").then(setVariable("var0", 1)).goTo("first-dialog");
		builder.on(talkToNpc(203220)).from("first-dialog").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 1)).then(setVariable("var0", 2)).goTo("second-dialog");
		builder.on(itemPlay(182214024, 0)).from("second-dialog").when(statusIs(QuestStatus.START))
			.when(variableIs("var0", 2)).then(setStatus(QuestStatus.REWARD)).goTo("item-played");
		return builder;
	}

	private static QuestDsl.QuestBuilder base(int id, String name, String category) {
		return quest(id).metadata(QuestMetadata.minimal(name, 0, category));
	}
}
