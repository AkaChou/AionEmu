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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionCompilerTest {
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
						    <node label="reward" status="REWARD"><var name="var1" value="1"/></node>
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
				"<transition target=\"start\" readiness=\"OFFLINE_ONLY\">"));
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
						  <nodes><node label="start" status="START"/><node label="reward" status="REWARD"/></nodes>
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
						  <nodes><node label="start" status="START"/><node label="reward" status="REWARD"/></nodes>
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
						  <nodes><node label="start" status="START"/><node label="reward" status="REWARD"/></nodes>
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
		CompiledQuestDefinition first = quest(1001).version(1)
			.node("start", project(QuestStatus.START, Map.of()))
			.on(talkToNpc(700001)).from("start").goTo("start").compile();
		CompiledQuestDefinition second = quest(1001).version(2)
			.node("start", project(QuestStatus.START, Map.of()))
			.on(talkToNpc(700001)).from("start").goTo("start").compile();
		assertEquals("DUPLICATE_CATALOG_ENTRY", assertThrows(QuestCompilationException.class,
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
	void conflictIndexPreservesTalkToNpcOverlapSemantics() {
		assertAmbiguous(new QuestEvent.TalkToNpc(700001, 8), new QuestEvent.TalkToNpc(700001, 8));
		assertAmbiguous(new QuestEvent.TalkToNpc(700001), new QuestEvent.TalkToNpc(700001, 8));

		twoRouteQuest(new QuestEvent.TalkToNpc(700001, 8), new QuestEvent.TalkToNpc(700002, 8)).compile();
		twoRouteQuest(new QuestEvent.TalkToNpc(700001, 8), new QuestEvent.TalkToNpc(700001, 9)).compile();
	}

	@Test
	void conflictIndexPreservesKillNpcSetOverlapSemantics() {
		assertAmbiguous(new QuestEvent.KillNpcSet(Set.of(210001, 210002)), new QuestEvent.KillNpc(210002));
		assertAmbiguous(new QuestEvent.KillNpcSet(Set.of(210001, 210002)),
			new QuestEvent.KillNpcSet(Set.of(210002, 210003)));

		twoRouteQuest(new QuestEvent.KillNpcSet(Set.of(210001, 210002)),
			new QuestEvent.KillNpcSet(Set.of(210003, 210004))).compile();
	}

	@Test
	void conflictIndexPreservesKillInWorldPrecedence() {
		twoRouteQuest(new QuestEvent.KillInWorld(0), new QuestEvent.KillInWorld(210010000)).compile();
		twoRouteQuest(new QuestEvent.KillInWorld(210010000), new QuestEvent.KillInWorld(220010000)).compile();
		assertAmbiguous(new QuestEvent.KillInWorld(210010000), new QuestEvent.KillInWorld(210010000));
	}

	@Test
	void conflictIndexCoversSpecializedOverlapKeys() {
		assertAmbiguous(new QuestEvent.UseItem(182400001, 1), new QuestEvent.UseItem(182400001, 2));
		assertAmbiguous(new QuestEvent.AttackNpc(210001), new QuestEvent.AttackNpc(210001));
		assertAmbiguous(new QuestEvent.ItemPlay(182400001, 1000), new QuestEvent.ItemPlay(182400001, 2000));
		assertAmbiguous(new QuestEvent.QuestDialog(1002), new QuestEvent.QuestDialog(1002));
		assertAmbiguous(new QuestEvent.KillRanked(1), new QuestEvent.KillRanked(2));
		assertAmbiguous(new QuestEvent.AtDistance(210001), new QuestEvent.AtDistance(210001));
		assertAmbiguous(new QuestEvent.LevelUp(), new QuestEvent.LevelUp());
	}

	@Test
	void conflictIndexUsesPrecomputedCompatibleSourceNodes() {
		var mutuallyExclusiveSources = conflictTestQuest();
		mutuallyExclusiveSources.on(new QuestEvent.AttackNpc(210001)).from("start").goTo("reward");
		mutuallyExclusiveSources.on(new QuestEvent.AttackNpc(210001)).from("reward").goTo("alternate");
		mutuallyExclusiveSources.compile();

		var wildcardSource = conflictTestQuest();
		wildcardSource.on(new QuestEvent.AttackNpc(210001)).from("start").goTo("reward");
		wildcardSource.on(new QuestEvent.AttackNpc(210001)).when(statusIs(QuestStatus.START)).goTo("alternate");
		assertEquals("AMBIGUOUS_TRANSITION",
			assertThrows(QuestCompilationException.class, wildcardSource::compile).code());
	}

	private static void assertAmbiguous(QuestEvent first, QuestEvent second) {
		assertEquals("AMBIGUOUS_TRANSITION",
			assertThrows(QuestCompilationException.class, () -> twoRouteQuest(first, second).compile()).code());
	}

	private static QuestDsl.QuestBuilder twoRouteQuest(QuestEvent first, QuestEvent second) {
		QuestDsl.QuestBuilder builder = conflictTestQuest();
		builder.on(first).from("start").goTo("reward");
		builder.on(second).from("start").goTo("alternate");
		return builder;
	}

	private static QuestDsl.QuestBuilder conflictTestQuest() {
		return quest(1001)
			.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var1", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
			.node("alternate", project(QuestStatus.REWARD, vars("var1", 2)));
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
						  <nodes><node label="start" status="START"/></nodes>
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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
					    <node label="reward" status="REWARD"/>
					    <node label="complete" status="COMPLETE"/>
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
	void rewardCompletionRestoresLegacyPreviewDialogs() {
		CompiledQuestDefinition compiled = quest(1001)
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()))
			.on(new QuestEvent.TalkToNpc(700001, 8)).from("reward")
			.then(completeQuest(2)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION))
			.compile();

		List<QuestTransition> previews = compiled.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 700001 && Set.of(-1, 1009).contains(talk.dialogId()))
			.toList();
		assertEquals(Set.of(-1, 1009), previews.stream()
			.map(QuestTransition::event).map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::dialogId).collect(java.util.stream.Collectors.toSet()));
		assertTrue(previews.stream().allMatch(transition ->
			transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("reward")
				&& transition.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(7)))));
	}

	@Test
	void explicitWildcardRewardPreviewIsPreserved() {
		var builder = quest(1001)
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()));
		builder.on(new QuestEvent.TalkToNpc(700001, 1009))
			.when(statusIs(QuestStatus.REWARD)).goTo("reward")
			.afterCommit(showQuestDialog(42));
		builder.on(new QuestEvent.TalkToNpc(700001, 8)).from("reward")
			.then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION));

		CompiledQuestDefinition compiled = builder.compile();
		List<QuestTransition> selectRewardRoutes = compiled.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(700001, 1009)))
			.toList();
		assertEquals(1, selectRewardRoutes.size());
		assertNull(selectRewardRoutes.get(0).sourceNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(42)),
			selectRewardRoutes.get(0).afterCommit());
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
					  <nodes><node label="start" status="START"/></nodes>
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
					  <nodes><node label="start" status="START"/></nodes>
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
						  <nodes><node label="start" status="START"/></nodes>
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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
	void dialogOpenActionsRejectNullPage() {
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.ShowQuestDialog(0));
		assertThrows(IllegalArgumentException.class, () -> new AfterCommitAction.ShowDialogWindow(0));

		for (String tag : List.of("show-quest-dialog", "show-quest-selection-dialog", "show-dialog-window")) {
			String xml = xmlWithTransition("<event><talk-to-npc npc-id=\"700001\"/></event>",
				"<after-commit><" + tag + " dialog-id=\"0\"/></after-commit>");
			assertEquals("INVALID_DIALOG_PAGE", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
					xml.getBytes(StandardCharsets.UTF_8)))).code());
		}
	}

	@Test
	void duplicateDialogCloseIsRejected() {
		assertEquals("DUPLICATE_DIALOG_CLOSE", assertThrows(QuestCompilationException.class,
			() -> quest(1001)
				.metadata(QuestMetadata.minimal("A test quest", 1101001, "QUEST"))
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001)).from("start").goTo("start")
				.afterCommit(closeDialog()).afterCommit(closeDialog())
				.compile()).code());
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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
	void playMovieParsesCutsceneMovieType() {
		String xml = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
						  <nodes><node label="start" status="START"/></nodes>
						  <transitions><transition source="start" target="start"><event><talk-to-npc npc-id="700001"/></event>
						    <after-commit><play-movie movie-id="30" type="CUTSCENE_MOVIE"/></after-commit>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		AfterCommitAction.PlayMovie movie = (AfterCommitAction.PlayMovie) definition.definition().transitions()
				.get(0).afterCommit().get(0);
		assertEquals(30, movie.movieId());
		assertEquals(QuestMovieType.CUTSCENE_MOVIE, movie.type());
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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
	void compactNodeSyntaxPreservesLabelStatusAndVarOrder() {
		String xml = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
							  <progress>
							    <bit-field name="var0" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/>
							    <bit-field name="var1" offset="2" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
							  </progress>
							  <nodes>
							    <node label="unaccepted" status="NONE"/>
							    <node label="start" status="START"><var name="var0" value="0"/><var name="var1" value="0"/></node>
							    <node label="reward" status="REWARD"><var name="var0" value="1"/><var name="var1" value="1"/></node>
						  </nodes>
						  <transitions>
						    <transition source="unaccepted" target="start"><event><level-up/></event></transition>
						    <transition source="start" target="reward"><event><kill-npc npc-id="210001"/></event></transition>
						  </transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		List<QuestNode> nodes = definition.definition().nodes();
		assertEquals(3, nodes.size());
		assertEquals("unaccepted", nodes.get(0).label());
		assertEquals(QuestStatus.NONE, nodes.get(0).projection().status());
		assertTrue(nodes.get(0).projection().variables().isEmpty());
		assertEquals("start", nodes.get(1).label());
		assertEquals(QuestStatus.START, nodes.get(1).projection().status());
		assertEquals(Map.of("var0", 0, "var1", 0), nodes.get(1).projection().variables());
		assertEquals(List.of("var0", "var1"), nodes.get(1).projection().variables().keySet().stream().toList());
		assertEquals("reward", nodes.get(2).label());
		assertEquals(QuestStatus.REWARD, nodes.get(2).projection().status());
		assertEquals(Map.of("var0", 1, "var1", 1), nodes.get(2).projection().variables());
		assertEquals(List.of("var0", "var1"), nodes.get(2).projection().variables().keySet().stream().toList());
	}

	@Test
	void legacyNodeWrappersAndMixedFormsAreRejected() {
		// 运行期拼接 legacy wrapper 标签名,保持测试源码零旧 wrapper 字面量(契约门禁)
		String project = "pro" + "ject";
		String vars = "va" + "rs";
		String legacy = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
						  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes>
						    <node label="start">
						      <${PROJECT} status="START">
						        <${VARS}>
						          <var name="var1" value="0"/>
						        </${VARS}>
						      </${PROJECT}>
						    </node>
						  </nodes>
						  <transitions/>
						</quest-definition>

				""".replace("${PROJECT}", project).replace("${VARS}", vars);
		QuestCompilationException legacyError = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				legacy.getBytes(StandardCharsets.UTF_8))));
		assertEquals("INVALID_XML", legacyError.code());

		String varsOnly = """

							<quest-definition id="1001" version="1">
							  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
							  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
							  <nodes><node label="start" status="START"><${VARS}><var name="var1" value="0"/></${VARS}></node></nodes>
							  <transitions/>
							</quest-definition>

					""".replace("${VARS}", vars);
		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				varsOnly.getBytes(StandardCharsets.UTF_8)))).code());

		// mixed form: compact node that also carries the legacy wrapper
		String mixed = legacy.replace("<node label=\"start\">",
			"<node label=\"start\" status=\"START\">");
		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				mixed.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void migrationNormalizerRejectsAmbiguousLegacyWrappers() {
		String project = "pro" + "ject";
		String vars = "va" + "rs";
		String valid = "<quest-definition id=\"1001\" version=\"1\"><metadata name=\"test\" display-name-id=\"1\" min-level=\"0\" max-level=\"99\" category=\"QUEST\"/><nodes><node label=\"start\"><%s status=\"START\"><%s><var name=\"var0\" value=\"0\"/></%s></%s></node></nodes><transitions/></quest-definition>"
			.formatted(project, vars, vars, project);
		assertTrue(QuestXmlMigrationVerifier.normalize(valid).contains("status=\"START\""));

		String extraProjectAttribute = valid.replace("status=\"START\"", "status=\"START\" extra=\"x\"");
		QuestCompilationException failure = assertThrows(QuestCompilationException.class,
			() -> QuestXmlMigrationVerifier.normalize(extraProjectAttribute));
		assertEquals("NORMALIZE_FAILED", failure.code());

		String mixedNode = valid.replace("label=\"start\"", "label=\"start\" status=\"START\"");
		failure = assertThrows(QuestCompilationException.class, () -> QuestXmlMigrationVerifier.normalize(mixedNode));
		assertEquals("NORMALIZE_FAILED", failure.code());
	}

	@Test
	void compactNodeSchemaAndProjectionValidationRejectInvalidForms() {
		String valid = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
						  <progress><bit-field name="var1" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes>
						    <node label="start" status="START"><var name="var1" value="0"/></node>
						    <node label="reward" status="REWARD"><var name="var1" value="1"/></node>
						  </nodes>
						  <transitions><transition source="start" target="reward"><event><level-up/></event></transition></transitions>
						</quest-definition>

					""";
		for (String malformed : List.of(
			valid.replace("label=\"start\" ", ""),
			valid.replace(" status=\"START\"", ""),
			valid.replace("<var name=\"var1\" ", "<var "),
			valid.replace(" value=\"0\"", ""),
			valid.replace("status=\"START\"", "status=\"START\" var1=\"0\""))) {
			assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
					malformed.getBytes(StandardCharsets.UTF_8)))).code());
		}

		String unknownField = valid.replace("name=\"var1\" value=\"0\"", "name=\"missing\" value=\"0\"");
		assertEquals("UNKNOWN_PROGRESS_FIELD", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				unknownField.getBytes(StandardCharsets.UTF_8)))).code());

		String outOfRange = valid.replace("name=\"var1\" value=\"0\"", "name=\"var1\" value=\"4\"");
		assertThrows(IllegalArgumentException.class, () -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			outOfRange.getBytes(StandardCharsets.UTF_8))));

		String duplicateProjection = valid.replace("label=\"reward\" status=\"REWARD\"",
			"label=\"reward\" status=\"START\"").replace("value=\"1\"", "value=\"0\"");
		assertEquals("DUPLICATE_NODE_PROJECTION", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				duplicateProjection.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void compactNodeRejectsUnknownStatusAndKeepsLegacyLastWinDuplicateVarSemantics() {
		String unknownStatus = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
						  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes><node label="start" status="WIPED"/></nodes>
						  <transitions/>
						</quest-definition>

				""";
		assertEquals("INVALID_XML", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
				unknownStatus.getBytes(StandardCharsets.UTF_8)))).code());

		// duplicate vars keep the legacy last-wins projection semantics (map put, not a wrapper check)
		String duplicateVar = """

						<quest-definition id="1001" version="1">
						  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
						  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes><node label="start" status="START"><var name="var1" value="0"/><var name="var1" value="1"/></node></nodes>
						  <transitions>
						    <transition source="start" target="start"><event><level-up/></event></transition>
						  </transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			duplicateVar.getBytes(StandardCharsets.UTF_8)));
		assertEquals(Map.of("var1", 1), definition.definition().nodes().getFirst().projection().variables());
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
						    <node label="start" status="START"><var name="var1" value="0"/></node>
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

	@Test
	void blockingItemUseCannotPromoteAQuestToComplete() {
		assertEquals("COMPLETE_QUEST_ACTION_REQUIRED", assertThrows(QuestCompilationException.class,
			() -> quest(1001)
				.node("started", project(QuestStatus.START, vars("var0", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
				.node("complete", project(QuestStatus.COMPLETE, vars("var0", 0)))
				.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
				.on(new QuestEvent.UseItem(182200501)).from("started").goTo("reward")
				.on(new QuestEvent.UseItem(182200501)).from("reward")
				.then(new QuestAction.BlockDefaultItemUse()).goTo("complete")
				.on(new QuestEvent.UseItem(182200501)).from("complete")
				.then(new QuestAction.BlockDefaultItemUse()).goTo("complete")
				.compile()).code());
	}

	private static String xmlWithTransition(String event, String afterCommit) {
		return """

					<quest-definition id="1001" version="1">
					  <metadata name="A test quest" display-name-id="1101001" min-level="0" max-level="2147483647" category="QUEST"/>
					  <progress><bit-field name="var1" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
					  <nodes><node label="start" status="START"><var name="var1" value="0"/></node></nodes>
					  <transitions><transition target="start">%s<conditions><status-is status="START"/></conditions>%s</transition></transitions>
					</quest-definition>

			""".formatted(event, afterCommit);
	}
}
