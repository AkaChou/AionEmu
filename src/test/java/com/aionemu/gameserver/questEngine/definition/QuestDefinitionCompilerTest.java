package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.attackTarget;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.cancelQuestTimer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.closeDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.completeQuest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.despawnNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.playMovie;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.grantQuestBaseReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.showQuestDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.showQuestSelectionDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.spawnNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.spawnNpcInInstance;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startFollow;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startEligible;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startInvisibleTimer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startQuestTimer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.startWalking;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.stopFollow;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.teleportPlayer;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.hasItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.refreshPlayerStats;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCompilerTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "quest/1001", "fixture");

	@Test
	void xmlAndDslCompileToTheSameImmutableDefinition() {
		QuestMetadata metadata = QuestMetadata.minimal("A test quest", 1101001, "QUEST");
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(metadata)
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.when(hasItem(182400001, 5))
				.then(removeItem(182400001, 5))
				.then(setVariable("var1", 1))
				.goTo("reward")
				.afterCommit(closeDialog())
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				    <node label="reward"><project status="REWARD"><vars><var name="var1" value="1"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="reward">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/><has-item item-id="182400001" count="5"/></conditions>
				      <actions><remove-item item-id="182400001" count="5"/><set-variable field="var1" value="1"/></actions>
				      <after-commit><close-dialog/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
		assertEquals(List.of("TALK_TO_NPC"), fromXml.transitionsByType().keySet().stream().toList());
	}

	@Test
	void taskXmlRejectsMigrationAnnotations() {
		String valid = xmlWithTransition("<event><talk-to-npc npc-id=\"700001\"/></event>", "");
		List<String> invalid = List.of(
			valid.replace("version=\"1\"", "version=\"1\" ownership=\"CURRENT\""),
			valid.replace("<metadata", "<evidence><ref source=\"test\" locator=\"x\" statement=\"y\"/></evidence><metadata"),
			valid.replace("<transition target=\"start\">",
				"<transition target=\"start\" shadow-coverage=\"OFFLINE_ONLY\">"));
		for (String xml : invalid) {
			assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
					xml.getBytes(StandardCharsets.UTF_8)))).code());
		}
	}

	@Test
	void talkDialogIdSetsExpandToTypedTransitionsAndRejectAmbiguity() {
		String expanded = xmlWithTransition(
			"<event><talk-to-npc npc-id=\"700001\" dialog-ids=\"-1 8..10 2147483646..2147483647\"/></event>", "");
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			expanded.getBytes(StandardCharsets.UTF_8)));
		assertEquals(List.of(-1, 8, 9, 10, 2147483646, 2147483647),
			definition.definition().transitions().stream()
			.map(QuestTransition::event).map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::dialogId).toList());

		Map<String, String> invalid = Map.of(
			"AMBIGUOUS_DIALOG_EVENT",
			"<event><talk-to-npc npc-id=\"700001\" dialog-id=\"8\" dialog-ids=\"8..10\"/></event>",
			"INVALID_DIALOG_ID_RANGE",
			"<event><talk-to-npc npc-id=\"700001\" dialog-ids=\"10..8\"/></event>",
			"DUPLICATE_DIALOG_ID",
			"<event><talk-to-npc npc-id=\"700001\" dialog-ids=\"8 8\"/></event>");
		for (Map.Entry<String, String> entry : invalid.entrySet()) {
			assertEquals(entry.getKey(), assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
					xmlWithTransition(entry.getValue(), "").getBytes(StandardCharsets.UTF_8)))).code());
		}
	}

	@Test
	void extendedEventFamilyUsesTheSameTypedXmlAndDslFrontEnds() {
		CompiledQuestDefinition fromDsl = quest(1004)
			.metadata(QuestMetadata.minimal("a", 1, "QUEST"))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(new QuestEvent.MovieEnd(9)).when(statusIs(QuestStatus.START)).goTo("reward").compile();
		String xml = """
				<quest-definition id="1004" version="1">
				  <metadata name="a" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
				  <nodes><node label="start"><project status="START"/></node><node label="reward"><project status="REWARD"/></node></nodes>
				  <transitions><transition target="reward"><event><movie-end movie-id="9"/></event><conditions><status-is status="START"/></conditions></transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(fromDsl.definition(), fromXml.definition());
		assertEquals(List.of("MOVIE_END"), fromXml.transitionsByType().keySet().stream().toList());
	}

	@Test
	void escortEventFamilyCompilesThroughXmlAndDsl() {
		CompiledQuestDefinition reachDsl = quest(1005)
			.metadata(QuestMetadata.minimal("a", 1, "QUEST"))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(new QuestEvent.NpcReachTarget()).when(statusIs(QuestStatus.START)).goTo("reward").compile();
		String reachXml = """
				<quest-definition id="1005" version="1">
				  <metadata name="a" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
				  <nodes><node label="start"><project status="START"/></node><node label="reward"><project status="REWARD"/></node></nodes>
				  <transitions><transition target="reward"><event><npc-reach-target/></event><conditions><status-is status="START"/></conditions></transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition reachFromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(reachXml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(reachDsl.definition(), reachFromXml.definition());
		assertEquals(List.of("NPC_REACH_TARGET"), reachFromXml.transitionsByType().keySet().stream().toList());

		CompiledQuestDefinition lostDsl = quest(1006)
			.metadata(QuestMetadata.minimal("a", 1, "QUEST"))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(new QuestEvent.NpcLostTarget()).when(statusIs(QuestStatus.START)).goTo("reward").compile();
		String lostXml = """
				<quest-definition id="1006" version="1">
				  <metadata name="a" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
				  <nodes><node label="start"><project status="START"/></node><node label="reward"><project status="REWARD"/></node></nodes>
				  <transitions><transition target="reward"><event><npc-lost-target/></event><conditions><status-is status="START"/></conditions></transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition lostFromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(lostXml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(lostDsl.definition(), lostFromXml.definition());
		assertEquals(List.of("NPC_LOST_TARGET"), lostFromXml.transitionsByType().keySet().stream().toList());
	}

	@Test
	void overlappingBitFieldsFailClosed() {
		assertThrows(IllegalArgumentException.class, () -> ProgressLayout.of(List.of(
				new BitField("a", 0, 6, PersistenceMode.PERSISTENT),
				new BitField("b", 5, 6, PersistenceMode.PERSISTENT))));
		assertThrows(IllegalArgumentException.class,
				() -> new BitField("too-wide", 30, 4, PersistenceMode.PERSISTENT));
	}

	@Test
	void duplicateOwnerFailsClosed() {
		CompiledQuestDefinition first = QuestDefinitionCompiler.compile(QuestDefinition.catalogOnly(1001, 1,
				QuestMetadata.minimal("a", 1, "QUEST"), List.of(EVIDENCE)));
		CompiledQuestDefinition second = QuestDefinitionCompiler.compile(QuestDefinition.catalogOnly(1001, 2,
				QuestMetadata.minimal("b", 2, "QUEST"), List.of(EVIDENCE)));
		assertEquals("DUPLICATE_OWNER", assertThrows(QuestCompilationException.class,
				() -> new ImmutableQuestCatalog(List.of(first, second))).code());
	}

	@Test
	void badReferenceAndUnreachableNodeFailClosed() {
		assertEquals("BAD_NODE_REFERENCE", assertThrows(QuestCompilationException.class, () -> quest(1001)
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).goTo("missing").compile()).code());

		assertEquals("UNREACHABLE_NODE", assertThrows(QuestCompilationException.class, () -> quest(1001)
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("orphan", project(QuestStatus.REWARD, vars("var1", 1)))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).goTo("start").compile()).code());
	}

	@Test
	void executableDefinitionWithoutTransitionsFailsClosed() {
		assertEquals("NO_TRANSITIONS", assertThrows(QuestCompilationException.class, () -> quest(1001)
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.compile()).code());
	}

	@Test
	void ambiguousTransitionsRequireUniqueExplicitPriorities() {
		var builder = quest(1001)
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
				.node("complete", project(QuestStatus.COMPLETE, vars("var1", 2)))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT));
		builder.on(talkToNpc(700001)).from("start").goTo("reward");
		builder.on(talkToNpc(700001)).from("start").then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION));
		assertEquals("AMBIGUOUS_TRANSITION", assertThrows(QuestCompilationException.class, builder::compile).code());
	}

	@Test
	void catalogOnlyCannotCarryExecution() {
		QuestDefinition definition = new QuestDefinition(1001, 1, QuestOwnership.CATALOG_ONLY,
				List.of(EVIDENCE), QuestMetadata.minimal("catalog", 1, "QUEST"), ProgressLayout.empty(),
				List.of(new QuestNode("start", new NodeProjection(QuestStatus.START, java.util.Map.of()))), List.of());
		assertEquals("CATALOG_ONLY_EXECUTION", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionCompiler.compile(definition)).code());
	}

	@Test
	void duplicateRuntimeProjectionFailsClosed() {
		assertEquals("DUPLICATE_NODE_PROJECTION", assertThrows(QuestCompilationException.class, () -> quest(1001)
				.node("first", project(QuestStatus.START, vars("var1", 0)))
				.node("second", project(QuestStatus.START, vars("var1", 0)))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.on(talkToNpc(700001)).from("first").goTo("second").compile()).code());
	}

	@Test
	void unknownXmlActionIsRejected() {
		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="a" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
				  <nodes><node label="start"><project status="START"/></node></nodes>
				  <transitions><transition target="start"><event><talk-to-npc npc-id="1"/></event><actions><arbitrary-service-call/></actions></transition></transitions>
				</quest-definition>
				""";
		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void showQuestDialogCompilesIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(showQuestDialog(1011))
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><show-quest-dialog dialog-id="1011"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void typedCompletionLifecycleCompilesIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
			.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()))
			.on(new QuestEvent.TalkToNpc(700001, 8)).from("reward")
			.when(statusIs(QuestStatus.REWARD)).when(startEligible())
			.then(grantQuestBaseReward("GOLD", 0, 120)).then(completeQuest(0))
			.goTo("complete").afterCommit(refreshPlayerStats())
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION))
			.afterCommit(showQuestSelectionDialog(10))
			.compile();

		String xml = """
				<quest-definition id="1001" version="1">
			  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
			  <nodes>
			    <node label="reward"><project status="REWARD"/></node>
			    <node label="complete"><project status="COMPLETE"/></node>
			  </nodes>
			  <transitions><transition source="reward" target="complete">
			    <event><talk-to-npc npc-id="700001" dialog-id="8"/></event>
			    <conditions><status-is status="REWARD"/><start-eligible/></conditions>
			    <actions><grant-reward kind="GOLD" id="0" amount="120" amount-mode="QUEST_BASE"/><complete-quest reward-index="0"/></actions>
			    <after-commit><refresh-player-stats/><sync-quest-state mode="COMPLETION"/><show-quest-selection-dialog dialog-id="10"/></after-commit>
			  </transition></transitions>
			</quest-definition>
			""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void completeProjectionRequiresTheCompletionStateSyncMode() {
		QuestCompilationException failure = assertThrows(QuestCompilationException.class, () -> quest(1001)
			.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()))
			.on(talkToNpc(700001)).from("reward").then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.PACKET_ONLY))
			.compile());

		assertEquals("COMPLETE_QUEST_SYNC_REQUIRED", failure.code());
	}

	@Test
	void rewardAmountModeIsRestrictedByTheXmlSchema() {
		String xml = """
				<quest-definition id="1001" version="1">
			  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
			  <nodes><node label="start"><project status="START"/></node></nodes>
			  <transitions><transition target="start"><event><talk-to-npc npc-id="700001"/></event>
			    <actions><grant-reward kind="GOLD" id="0" amount="120" amount-mode="SCALED"/></actions>
			  </transition></transitions>
			</quest-definition>
			""";

		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void questStateSyncModeIsRequiredByTheXmlSchema() {
		String xml = """
				<quest-definition id="1001" version="1">
			  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
			  <nodes><node label="start"><project status="START"/></node></nodes>
			  <transitions><transition target="start"><event><talk-to-npc npc-id="700001"/></event>
			    <after-commit><sync-quest-state/></after-commit>
			  </transition></transitions>
			</quest-definition>
			""";

		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void unknownAfterCommitActionIsRejected() {
		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="a" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
				  <nodes><node label="start"><project status="START"/></node></nodes>
				  <transitions><transition target="start"><event><talk-to-npc npc-id="1"/></event><after-commit><arbitrary-effect/></after-commit></transition></transitions>
				</quest-definition>
				""";
		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void teleportPlayerCompilesIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(teleportPlayer(110010000, 1474f, 1352f, 564f, (byte) 21))
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><teleport-player-current-or-default world-id="110010000" x="1474" y="1352" z="564" heading="21"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void invalidTeleportCoordinatesAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.TeleportPlayer(110010000, Float.NaN, 0f, 0f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.TeleportPlayer(0, 1f, 2f, 3f, (byte) 0));
	}

	@Test
	void playMovieCompilesIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(playMovie(12345))
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><play-movie movie-id="12345"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void playMovieRejectsNonPositiveIds() {
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.PlayMovie(0));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.PlayMovie(-5));
	}

	@Test
	void spawnNpcCompilesIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(spawnNpc("guardian", 310040000, 204830, 1f, 2f, 3f, (byte) 95))
				.afterCommit(spawnNpcInInstance("fixed", 310040000, 37, 204831, 4f, 5f, 6f, (byte) 7))
				.afterCommit(despawnNpc("guardian"))
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><spawn-npc-current-or-default slot="guardian" world-id="310040000" template-id="204830" x="1" y="2" z="3" heading="95"/><spawn-npc-fixed-instance slot="fixed" world-id="310040000" instance-id="37" template-id="204831" x="4" y="5" z="6" heading="7"/><despawn-npc slot="guardian"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void spawnAndDespawnRejectInvalidArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.SpawnNpc("", 310040000, 204830, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.SpawnNpc("guardian", 0, 204830, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.SpawnNpc("guardian", 310040000, 0, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
				() -> new AfterCommitAction.SpawnNpc("guardian", 310040000, 204830, Float.NaN, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.DespawnNpc(" "));
	}

	@Test
	void aiCommandsCompileIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(startFollow("guardian"))
				.afterCommit(stopFollow("guardian"))
				.afterCommit(attackTarget("guardian"))
				.afterCommit(startWalking("guardian"))
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><start-follow slot="guardian"/><stop-follow slot="guardian"/><attack-target slot="guardian"/><start-walking slot="guardian"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void aiCommandsRejectBlankSlot() {
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.StartFollow(" "));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.StopFollow(""));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.AttackTarget(null));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.StartWalking(" "));
	}

	@Test
	void questTimersCompileIdenticallyThroughXmlAndDsl() {
		CompiledQuestDefinition fromDsl = quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001))
				.when(statusIs(QuestStatus.START))
				.goTo("start")
				.afterCommit(startQuestTimer(300))
				.afterCommit(startInvisibleTimer(60))
				.afterCommit(cancelQuestTimer())
				.compile();

		String xml = """
				<quest-definition id="1001" version="1">
				  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes>
				    <node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node>
				  </nodes>
				  <transitions>
				    <transition target="start">
				      <event><talk-to-npc npc-id="700001"/></event>
				      <conditions><status-is status="START"/></conditions>
				      <after-commit><start-quest-timer seconds="300" timer-id="visible" scope="PLAYER_QUEST" persistence="SESSION" overwrite="REPLACE" delivery="AT_MOST_ONCE"/><start-invisible-timer seconds="60" timer-id="invisible" scope="PLAYER_QUEST" persistence="SESSION" overwrite="REPLACE" delivery="AT_MOST_ONCE"/><cancel-quest-timer timer-id="visible" scope="PLAYER_QUEST"/></after-commit>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void questTimersRejectNonPositiveSeconds() {
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.StartQuestTimer(0));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.StartInvisibleTimer(-1));
	}

	@Test
	void customTimerPolicyCompilesIdenticallyThroughXmlAndDsl() {
		QuestTimerPolicy policy = QuestTimerPolicy.session("cinematic",
			QuestTimerPolicy.OverwritePolicy.KEEP_EXISTING);
		CompiledQuestDefinition fromDsl = quest(1001)
			.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
			.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var1", 0)))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).goTo("start")
			.afterCommit(startQuestTimer(15, policy))
			.afterCommit(cancelQuestTimer(policy.identity()))
			.compile();

		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			xmlWithTransition("<event><talk-to-npc npc-id=\"700001\"/></event>",
				"<after-commit><start-quest-timer seconds=\"15\" timer-id=\"cinematic\" scope=\"PLAYER_QUEST\" persistence=\"SESSION\" overwrite=\"KEEP_EXISTING\" delivery=\"AT_MOST_ONCE\"/><cancel-quest-timer timer-id=\"cinematic\" scope=\"PLAYER_QUEST\"/></after-commit>")
				.getBytes(StandardCharsets.UTF_8)));

		assertEquals(fromDsl.definition(), fromXml.definition());
	}

	@Test
	void xsdRejectsLegacyFreeTypeAndAmbiguousSpawnShapes() {
		QuestCompilationException oldEvent = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				xmlWithTransition("<event type=\"TALK_TO_NPC\" npc-id=\"700001\"/>", "")
					.getBytes(StandardCharsets.UTF_8))));
		assertEquals("INVALID_XML", oldEvent.code());

		QuestCompilationException oldSpawn = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				xmlWithTransition("<event><talk-to-npc npc-id=\"700001\"/></event>",
					"<after-commit><spawn-npc-fixed slot=\"guardian\" world-id=\"310040000\" template-id=\"204830\" x=\"1\" y=\"2\" z=\"3\" heading=\"0\"/></after-commit>")
					.getBytes(StandardCharsets.UTF_8))));
		assertEquals("INVALID_XML", oldSpawn.code());

		QuestCompilationException missingInstance = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				xmlWithTransition("<event><talk-to-npc npc-id=\"700001\"/></event>",
					"<after-commit><spawn-npc-fixed-instance slot=\"guardian\" world-id=\"310040000\" template-id=\"204830\" x=\"1\" y=\"2\" z=\"3\" heading=\"0\"/></after-commit>")
					.getBytes(StandardCharsets.UTF_8))));
		assertEquals("INVALID_XML", missingInstance.code());
	}

	private static String xmlWithTransition(String event, String afterCommit) {
		return """
			<quest-definition id="1001" version="1">
			  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
			  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes><node label="start"><project status="START"><vars><var name="var1" value="0"/></vars></project></node></nodes>
			  <transitions><transition target="start">%s<conditions><status-is status="START"/></conditions>%s</transition></transitions>
			</quest-definition>
			""".formatted(event, afterCommit);
	}
}
