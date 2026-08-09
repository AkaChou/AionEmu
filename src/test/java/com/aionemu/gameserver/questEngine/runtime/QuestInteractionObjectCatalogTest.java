package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDropScope;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestInteractionObjectCatalogTest {
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
		try (InputStream input = resource("aion/data/static_data/npcs/npc_template.xml")) {
			var reader = factory.createXMLStreamReader(input);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT
						&& "npc_template".equals(reader.getLocalName())
						&& Integer.toString(npcId).equals(reader.getAttributeValue(null, "npc_id"))) {
					return reader.getAttributeValue(null, "ai");
				}
			}
		}
		throw new IllegalStateException("missing NPC template " + npcId);
	}

	private static InputStream resource(String path) {
		InputStream input = QuestInteractionObjectCatalogTest.class.getClassLoader().getResourceAsStream(path);
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}
}
