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

/** Candidate-only vertical fixtures for the remaining true-server Simple* families. */
class SimpleQuestFamilyDefinitionTest {
	@Test
	void simpleHunt1102XmlAndDslMatchAndAdvanceARealTargetGroup() throws Exception {
		CompiledQuestDefinition definition = assertEquivalent("simplehunt-1102.xml", simpleHunt1102().compile());
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1102, QuestStatus.START, 0, Map.of()),
			killNpc(210134), definition.definition().transitions().get(2));
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.START, plan.orElseThrow().nextStatus());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
	}

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
			"/quest-definition-candidates/" + resource)) {
			fromXml = QuestDefinitionXmlCompiler.compile(input);
		}
		assertEquals(fromDsl.definition(), fromXml.definition());
		assertEquals(QuestOwnership.COMPILED_CANDIDATE, fromXml.ownership());
		return fromXml;
	}

	private static QuestDsl.QuestBuilder simpleHunt1102() {
		QuestDsl.QuestBuilder builder = base(1102, "SimpleHunt 1102", "IMPORTANT",
				new EvidenceRef("RETAIL_SIMPLE_HUNT_XML", "58Server/Map/XML/Quest_SimpleHunt.xml#id[1102]", "the true-server hunt entry declares Kerubar targets and count1=3"),
				new EvidenceRef("RETAIL_SCRIPT_DLL", "58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:688-693; SimpleHuntQuest.cpp:46-82", "the hunt type creates grouped kill progress objects"),
				new EvidenceRef("CURRENT_XML_OWNER", "src/main/resources/aion/data/static_data/quest_script_data/poeta.xml#monster_hunt[1102]", "current owner supplies start NPC 203057, target NPCs 210133/210134 and var0 end value 3"))
			.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("one-kill", project(QuestStatus.START, vars("var0", 1)))
			.node("two-kills", project(QuestStatus.START, vars("var0", 2)))
			.node("target-count-reached", project(QuestStatus.START, vars("var0", 3)));
		builder.on(talkToNpc(203057)).from("unaccepted").goTo("started");
		addHuntStep(builder, "started", "one-kill", 0, 1);
		addHuntStep(builder, "one-kill", "two-kills", 1, 2);
		addHuntStep(builder, "two-kills", "target-count-reached", 2, 3);
		return builder;
	}

	private static void addHuntStep(QuestDsl.QuestBuilder builder, String source, String target, int current, int next) {
		for (int npcId : new int[]{210133, 210134}) {
			builder.on(killNpc(npcId)).from(source).when(statusIs(QuestStatus.START))
				.when(variableIs("var0", current)).then(setVariable("var0", next)).goTo(target);
		}
	}

	private static QuestDsl.QuestBuilder simpleSerialHunt9622() {
		QuestDsl.QuestBuilder builder = base(9622, "SimpleSerialHunt 9622", "QUEST",
				new EvidenceRef("RETAIL_SIMPLE_SERIAL_HUNT_XML", "58Server/Map/XML/Quest_SimpleSerialHunt.xml#id[9622]", "the true-server serial hunt entry declares Rebecca and five ordered target groups"),
				new EvidenceRef("RETAIL_SCRIPT_DLL", "58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:724-729; SimpleSerialHuntQuest.cpp:46-82", "serial_hunt is a distinct type with the same grouped progress construction path"),
				new EvidenceRef("RETAIL_NPC_TEMPLATES", "src/main/resources/aion/data/static_data/npcs/npc_template.xml#name_desc[DF4_DaQ_Owllau_As_52]", "the first ordered target resolves to NPC 216626 and Rebecca_5 resolves to NPC 203223"))
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
		QuestDsl.QuestBuilder builder = base(1103, "SimpleCollect 1103", "IMPORTANT",
				new EvidenceRef("RETAIL_SIMPLE_COLLECT_XML", "58Server/Map/XML/Quest_SimpleCollectItem.xml#id[1103]", "the true-server collect entry declares LF1_Cherubim_pouch"),
				new EvidenceRef("RETAIL_SCRIPT_DLL", "58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:697-703,1041-1049", "the collect type reads collect parameters and registers a collect object"),
				new EvidenceRef("CURRENT_XML_OWNER", "src/main/resources/aion/data/static_data/quest_script_data/poeta.xml#item_collecting[1103]", "current owner supplies start NPC 203057 and action item 700105"))
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
		QuestDsl.QuestBuilder builder = base(1107, "SimpleUseItem 1107", "QUEST",
				new EvidenceRef("RETAIL_SIMPLE_USE_ITEM_XML", "58Server/Map/XML/Quest_SimpleUseItem.xml#id[1107]", "the true-server entry declares ITEM_QUEST_1107A and Namus as reward NPC"),
				new EvidenceRef("RETAIL_SCRIPT_DLL", "58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:706-711,1050-1065", "the use_item type reads use-item parameters and initializes the common quest object"),
				new EvidenceRef("CURRENT_ITEM_TEMPLATE", "src/main/resources/aion/data/static_data/items/item/item_etc_templates.xml#questid[1107]", "ITEM_QUEST_1107A resolves to item 182200501"))
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
		QuestDsl.QuestBuilder builder = base(9623, "SimpleItemPlay 9623", "QUEST",
				new EvidenceRef("RETAIL_SIMPLE_ITEM_PLAY_XML", "58Server/Map/XML/Quest_SimpleItemPlay.xml#id[9623]", "the true-server item-play entry declares Rebecca_5, Rebecca_2, Rebecca_3 and ITEM_QUEST_9623A"),
				new EvidenceRef("RETAIL_SCRIPT_DLL", "58Server/server58-source/MainServer_ScriptDLL64/fun/fun_912.cpp:715-721,1071-1094", "the item_play type reads item-play parameters and initializes the common quest object"),
				new EvidenceRef("CURRENT_ITEM_TEMPLATE", "src/main/resources/aion/data/static_data/items/item/item_etc_templates.xml#questid[9623]", "ITEM_QUEST_9623A resolves to item 182214024"))
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

	private static QuestDsl.QuestBuilder base(int id, String name, String category, EvidenceRef... evidence) {
		return quest(id).ownership(QuestOwnership.COMPILED_CANDIDATE)
			.metadata(QuestMetadata.minimal(name, 0, category))
			.evidence(evidence);
	}
}
