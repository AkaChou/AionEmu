package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDropScope;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestInteractionObjectCatalogTest {
	private static final List<String> NPC_SHARDS = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml",
		"npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
		"npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	@Test
	void productionQuestUseItemTalkRoutesDeclareActionEligibility() throws Exception {
		QuestCatalog catalog;
		try (InputStream input = resource(
				"aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
		}
		Set<Integer> questUseItemNpcs = questUseItemNpcIds();
		List<String> missing = new ArrayList<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			List<QuestTransition> transitions = definition.definition().transitions();
			for (var transition : transitions) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| (talk.dialogId() != null && talk.dialogId() != -1)
						|| !questUseItemNpcs.contains(talk.npcId())) {
					continue;
				}
				boolean started = definition.definition().nodes().stream()
					.anyMatch(node -> node.label().equals(transition.sourceNode())
						&& node.projection().status() == QuestStatus.START);
				if (!started) {
					continue;
				}
				boolean eligible = transitions.stream().anyMatch(candidate ->
					Objects.equals(candidate.sourceNode(), transition.sourceNode())
						&& candidate.event() instanceof QuestEvent.CanAct canAct
						&& canAct.templateId() == talk.npcId()
						&& "ACTION_ITEM_USE".equals(canAct.actionType()));
				if (!eligible) {
					missing.add(definition.id() + ":" + transition.sourceNode() + ":" + talk.npcId());
				}
			}
		}
		assertEquals(List.of(), missing);
	}

	@Test
	void productionQuestUseItemDropsDeclareActionEligibility() throws Exception {
		QuestCatalog catalog;
		try (InputStream input = resource(
				"aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
		}
		Set<Integer> questUseItemNpcs = questUseItemNpcIds();
		List<String> missing = new ArrayList<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			List<QuestTransition> transitions = definition.definition().transitions();
			for (QuestDrop drop : definition.definition().metadata().drops()) {
				if (drop.chance() <= 0 || !questUseItemNpcs.contains(drop.npcId())) {
					continue;
				}
				boolean eligible = transitions.stream().anyMatch(transition -> {
					if (!(transition.event() instanceof QuestEvent.CanAct canAct)
							|| canAct.templateId() != drop.npcId()
							|| !"ACTION_ITEM_USE".equals(canAct.actionType())) {
						return false;
					}
					return definition.definition().nodes().stream()
						.anyMatch(node -> node.label().equals(transition.sourceNode())
							&& node.projection().status() == QuestStatus.START
							&& (drop.collectingStep() == 0
								|| Objects.equals(node.projection().variables().get("var0"), drop.collectingStep())));
				});
				if (!eligible) {
					missing.add(definition.id() + ":" + drop.npcId() + ":" + drop.itemId()
						+ ":step=" + drop.collectingStep());
				}
			}
		}
		assertEquals(List.of(), missing);
	}

	@Test
	void wineBarrel1109UsesCatalogDropTalkAndActionObjectRoutes() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = resource(
				"aion/data/static_data/quest_definition/quests/1109.xml")) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}
		QuestProductionDispatcher dispatcher = dispatcher(definition);

		List<QuestCatalogDrop> drops = dispatcher.questDrops(700106);
		assertEquals(1, drops.size());
		assertEquals(1109, drops.get(0).questId());
		assertEquals(182200205, drops.get(0).itemId());
		assertEquals(QuestDropScope.GROUP, drops.get(0).scope());
		assertTrue(definition.definition().transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 700106));
		assertTrue(definition.definition().transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == 700106 && "ACTION_ITEM_USE".equals(canAct.actionType())));
		assertEquals("quest_use_item", npcAi(700106));
		assertDoesNotThrow(() -> QuestInteractionObjectValidator.validate(dispatcher,
			templateId -> templateId == 700106 ? "quest_use_item" : null));
	}

	@Test
	void dropBackedPureActionRouteProvidesTheTypedTalkFallbackContract() {
		QuestMetadata metadata = new QuestMetadata("interaction", 0, 1, 99, Set.of(), "QUEST",
			RepeatPolicy.once(), Set.of(), List.of(), List.of(),
			List.of(new QuestDrop(700106, 182200205, 100, true, 0)));
		CompiledQuestDefinition definition = QuestDsl.quest(990101).metadata(metadata)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.CanAct(700106, "ACTION_ITEM_USE")).from("started").goTo("started")
			.compile();

		assertDoesNotThrow(() -> QuestInteractionObjectValidator.validate(dispatcher(definition),
			templateId -> "quest_use_item"));
	}

	@Test
	void catalogDropWithoutActionEligibilityFailsStartupValidation() {
		QuestMetadata metadata = new QuestMetadata("interaction", 0, 1, 99, Set.of(), "QUEST",
			RepeatPolicy.once(), Set.of(), List.of(), List.of(),
			List.of(new QuestDrop(700106, 182200205, 100, true, 0)));
		CompiledQuestDefinition definition = QuestDsl.quest(990103).metadata(metadata)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.compile();

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> QuestInteractionObjectValidator.validate(dispatcher(definition),
				templateId -> "quest_use_item"));
		assertTrue(failure.getMessage().contains("catalog drop npc 700106"));
	}

	@Test
	void catalogDropCollectingStepMustMatchActionSource() {
		QuestMetadata metadata = new QuestMetadata("interaction", 0, 1, 99, Set.of(), "QUEST",
			RepeatPolicy.once(), Set.of(), List.of(), List.of(),
			List.of(new QuestDrop(700106, 182200205, 100, true, 1)));
		CompiledQuestDefinition definition = QuestDsl.quest(990104).metadata(metadata)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.CanAct(700106, "ACTION_ITEM_USE")).from("started").goTo("started")
			.compile();

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> QuestInteractionObjectValidator.validate(dispatcher(definition),
				templateId -> "quest_use_item"));
		assertTrue(failure.getMessage().contains("collecting step 1"));
	}

	@Test
	void questUseItemRouteWithoutTalkOrDropFailsStartupValidation() {
		CompiledQuestDefinition definition = QuestDsl.quest(990102)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.CanAct(700106, "ACTION_ITEM_USE")).from("started").goTo("started")
			.compile();

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> QuestInteractionObjectValidator.validate(dispatcher(definition),
				templateId -> "quest_use_item"));
		assertTrue(failure.getMessage().contains("neither TALK route nor catalog drop metadata"));
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition) {
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(List.of(definition)),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			(connection, playerId, questId, event) -> new QuestSnapshot(playerId, questId,
				QuestStatus.START, 0, Map.of()), new QuestActionPort() {
					@Override
					public void preflight(Connection connection, QuestSnapshot snapshot,
							List<com.aionemu.gameserver.questEngine.definition.QuestAction> required) {
					}

					@Override
					public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
							List<com.aionemu.gameserver.questEngine.definition.QuestAction> required) {
						return QuestTransactionParticipant.none();
					}
				}, new QuestStatePort() {
					@Override
					public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
					}

					@Override
					public void publish(int playerId, QuestMutationPlan plan) {
					}
				}, (action, snapshot, plan) -> { }, () -> connection(), ignored -> { },
			new QuestRuntimeMetricsCollector());
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				default -> method.getReturnType() == boolean.class ? false : null;
			});
	}

	private static String npcAi(int npcId) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		for (String shard : NPC_SHARDS) {
			try (InputStream input = resource("aion/data/static_data/npcs/" + shard)) {
				var reader = factory.createXMLStreamReader(input);
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT
							&& "npc_template".equals(reader.getLocalName())
							&& Integer.toString(npcId).equals(reader.getAttributeValue(null, "npc_id"))) {
						return reader.getAttributeValue(null, "ai");
					}
				}
			}
		}
		throw new IllegalStateException("missing NPC template " + npcId);
	}

	private static Set<Integer> questUseItemNpcIds() throws Exception {
		Set<Integer> result = new HashSet<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		for (String shard : NPC_SHARDS) {
			try (InputStream input = resource("aion/data/static_data/npcs/" + shard)) {
				var reader = factory.createXMLStreamReader(input);
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT
							&& "npc_template".equals(reader.getLocalName())
							&& "quest_use_item".equals(reader.getAttributeValue(null, "ai"))) {
						result.add(Integer.parseInt(reader.getAttributeValue(null, "npc_id")));
					}
				}
			}
		}
		return Set.copyOf(result);
	}

	private static InputStream resource(String path) {
		InputStream input = QuestInteractionObjectCatalogTest.class.getClassLoader().getResourceAsStream(path);
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}
}
