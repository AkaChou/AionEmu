package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestConditionEvaluator;
import com.aionemu.gameserver.questEngine.runtime.QuestEquipmentFacts;
import com.aionemu.gameserver.questEngine.runtime.QuestMembershipFacts;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestAdditionalCapabilityDefinitionTest {
	@Test
	void compilesAndEvaluatesEquipmentMembershipAndUnequipCapabilities() {
		String xml = """

						<quest-definition id="20043" version="1">
						  <metadata name="stigma-capabilities" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="204264" dialog-id="3058"/></event>
						    <conditions>
						      <equipped-item item-id="140000003" count="1"/>
						      <membership-permission permission="STIGMA_SLOT_QUEST"/>
						    </conditions>
						    <actions><unequip-item item-id="140000003" remove-count="1"/></actions>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.EquippedItem(140000003, 1, true),
			new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST)),
			transition.conditions());
		assertEquals(List.of(new QuestAction.UnequipItem(140000003, 1)), transition.actions());

		QuestSnapshot matching = new QuestSnapshot(7, 20043, QuestStatus.START, 0, Map.of())
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(), Map.of(140000003, 1)))
			.withMembershipFacts(new QuestMembershipFacts(Set.of(QuestMembershipPermission.STIGMA_SLOT_QUEST)));
		assertTrue(QuestConditionEvaluator.matches(compiled.definition().progressLayout(), matching,
			transition.conditions()));
		assertFalse(QuestConditionEvaluator.matches(compiled.definition().progressLayout(),
			matching.withEquipmentFacts(new QuestEquipmentFacts(Map.of(), Map.of())), transition.conditions()));
	}

	@Test
	void compilesAndEvaluatesNegativeInventoryCondition() {
		String xml = """

						<quest-definition id="20045" version="1">
						  <metadata name="missing-item" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799513"/></event>
						    <conditions><has-item item-id="182203009" count="1" expected="false"/></conditions>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestCondition condition = compiled.definition().transitions().get(0).conditions().get(0);
		assertEquals(new QuestCondition.HasItem(182203009, 1, false), condition);
		QuestSnapshot empty = new QuestSnapshot(7, 20045, QuestStatus.START, 0, Map.of());
		QuestSnapshot carrying = new QuestSnapshot(7, 20045, QuestStatus.START, 0,
			Map.of(182203009, 1));
		assertTrue(QuestConditionEvaluator.matches(compiled.definition().progressLayout(), empty,
			List.of(condition)));
		assertFalse(QuestConditionEvaluator.matches(compiled.definition().progressLayout(), carrying,
			List.of(condition)));
	}

	@Test
	void compilesSelectedRewardActionAndRejectsUnknownMetadataIndex() {
		String valid = """

						<quest-definition id="20033" version="1">
						  <metadata name="selected-reward" display-name-id="0" min-level="1" max-level="55" category="QUEST">
						    <rewards><reward kind="AP" id="0" amount="300"/><reward kind="AP" id="0" amount="600"/></rewards>
						  </metadata>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799513"/></event>
						    <actions><grant-selected-reward reward-index="1"/></actions>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(valid.getBytes(StandardCharsets.UTF_8)));
		assertEquals(new QuestAction.GrantSelectedReward(1),
			compiled.definition().transitions().get(0).actions().get(0));

		String invalid = valid.replace("reward-index=\"1\"", "reward-index=\"2\"");
		QuestCompilationException failure = assertThrows(QuestCompilationException.class, () ->
			QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))));
		assertEquals("SELECTED_REWARD_INDEX_OUT_OF_RANGE", failure.code());
	}
	@Test
	void compilesTeamAdvancedClassAndRawDialogCapabilities() {
		String xml = """

						<quest-definition id="20034" version="1">
						  <metadata name="capabilities" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799513"/></event>
						    <conditions>
						      <player-in-group/>
						      <advanced-class-is class="GLADIATOR"/>
						    </conditions>
						    <after-commit><show-dialog-window dialog-id="10"/></after-commit>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);

		assertEquals(new QuestCondition.PlayerInGroup(), transition.conditions().get(0));
		assertEquals(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR), transition.conditions().get(1));
		assertInstanceOf(AfterCommitAction.ShowDialogWindow.class, transition.afterCommit().get(0));
		assertEquals(10, ((AfterCommitAction.ShowDialogWindow) transition.afterCommit().get(0)).dialogId());
	}

	@Test
	void compilesConcretePlayerClassChangeAction() {
		String xml = """

						<quest-definition id="20037" version="1">
						  <metadata name="class-change" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799513"/></event>
						    <after-commit><set-class class="GLADIATOR"/></after-commit>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(new AfterCommitAction.SetPlayerClass(PlayerClass.GLADIATOR),
			compiled.definition().transitions().get(0).afterCommit().get(0));
	}

	@Test
	void compilesNpcHpThresholdForAttackEvents() {
		String xml = """

						<quest-definition id="20038" version="1">
						  <metadata name="attack-threshold" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><attack-npc npc-id="211043"/></event>
						    <conditions><npc-hp-below-percent npc-id="211043" percent="50"/></conditions>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(new QuestCondition.NpcHpBelowPercent(211043, 50),
			compiled.definition().transitions().get(0).conditions().get(0));
	}

	@Test
	void compilesCurrencyBalanceConditionAndDebitAction() {
		String xml = """

						<quest-definition id="20039" version="1">
						  <metadata name="currency" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="211043"/></event>
						    <conditions><currency-at-least kind="GOLD" amount="20000"/></conditions>
						    <actions><decrease-currency kind="GOLD" amount="20000"/></actions>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);

		assertEquals(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 20000),
			transition.conditions().get(0));
		assertEquals(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 20000),
			transition.actions().get(0));
	}

	@Test
	void compilesAndEvaluatesFinishedQuestConditionWithCapturedFacts() {
		String xml = """

						<quest-definition id="20042" version="1">
						  <metadata name="prerequisite-condition" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="211043"/></event>
						    <conditions><quests-finished quest-ids="14020 14021"/></conditions>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestCondition condition = compiled.definition().transitions().get(0).conditions().get(0);
		assertEquals(new QuestCondition.QuestsFinished(Set.of(14020, 14021)), condition);

		var layout = compiled.definition().progressLayout();
		var missing = new com.aionemu.gameserver.questEngine.runtime.QuestSnapshot(7, 20042,
			com.aionemu.gameserver.questEngine.model.QuestStatus.START, 0, java.util.Map.of())
			.withCompletedQuestIds(Set.of(14020));
		var complete = missing.withCompletedQuestIds(Set.of(14020, 14021));
		assertFalse(com.aionemu.gameserver.questEngine.runtime.QuestConditionEvaluator.matches(layout, missing,
			java.util.List.of(condition)));
		assertTrue(com.aionemu.gameserver.questEngine.runtime.QuestConditionEvaluator.matches(layout, complete,
			java.util.List.of(condition)));
	}

	@Test
	void compilesStrictCurrencyBelowCondition() {
		String xml = """

						<quest-definition id="20040" version="1">
						  <metadata name="currency-below" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="211043"/></event>
						    <conditions><currency-below kind="GOLD" amount="6500"/></conditions>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500),
			compiled.definition().transitions().get(0).conditions().get(0));
	}

	@Test
	void treatsOppositeFactBranchesAsMutuallyExclusiveTransitions() {
		String xml = """

						<quest-definition id="20035" version="1">
						  <metadata name="branches" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <progress><bit-field name="step" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes>
						    <node label="started" status="START"><var name="step" value="0"/></node>
						    <node label="grouped" status="START"><var name="step" value="1"/></node>
						    <node label="solo" status="START"><var name="step" value="2"/></node>
						  </nodes>
						  <transitions>
						    <transition source="started" target="grouped"><event><talk-to-npc npc-id="799513"/></event><conditions><player-in-group/></conditions></transition>
						    <transition source="started" target="solo"><event><talk-to-npc npc-id="799513"/></event><conditions><player-in-group expected="false"/></conditions></transition>
						  </transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(2, compiled.definition().transitions().size());
	}

	@Test
	void compilesResidentNpcCoordinateFollowAction() {
		String xml = """

						<quest-definition id="20036" version="1">
						  <metadata name="escort" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799036" dialog-id="10000"/></event>
						    <after-commit><start-follow-current-target x="292.63895" y="489.47452" z="574.2429"/></after-commit>
						  </transition></transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		AfterCommitAction.StartFollowCurrentTargetToPoint action =
				assertInstanceOf(AfterCommitAction.StartFollowCurrentTargetToPoint.class,
					compiled.definition().transitions().get(0).afterCommit().get(0));
		assertEquals(292.63895f, action.x());
		assertEquals(489.47452f, action.y());
		assertEquals(574.2429f, action.z());
	}

	@Test
	void compilesRandomSpawnAndWorldNpcAttackActions() {
		String xml = """

						<quest-definition id="20041" version="1">
						  <metadata name="defense" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><talk-to-npc npc-id="799513"/></event>
						    <after-commit>
						      <spawn-npc-random slot="defense-mob" replace-existing="true">
						        <variant template-id="213576" world-id="310040000" x="254.74" y="236.72" z="217.48" heading="95"/>
						        <variant template-id="213577" world-id="310040000" x="257.92" y="237.39" z="217.48" heading="95"/>
						      </spawn-npc-random>
						      <attack-npc-template slot="defense-mob" template-id="204044"/>
						    </after-commit>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		var afterCommit = compiled.definition().transitions().get(0).afterCommit();
		var spawn = assertInstanceOf(AfterCommitAction.SpawnNpcRandom.class, afterCommit.get(0));
		assertEquals(2, spawn.variants().size());
		assertTrue(spawn.replaceExisting());
		assertEquals(new AfterCommitAction.AttackNpcTemplate("defense-mob", 204044), afterCommit.get(1));
	}

	@Test
	void acceptsStatusBoundWildcardTransitionsAcrossMultipleQuestNodes() {
		String xml = """

						<quest-definition id="20044" version="1">
						  <metadata name="wildcard-source" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <progress><bit-field name="step" offset="0" width="7" min="0" max="98" persistence="PERSISTENT" scope="LOCAL"/></progress>
						  <nodes>
						    <node label="step93" status="START"><var name="step" value="93"/></node>
						    <node label="step94" status="START"><var name="step" value="94"/></node>
						  </nodes>
						  <transitions>
						    <transition source="step93" target="step94"><event><talk-to-npc npc-id="799513"/></event></transition>
						    <transition target="step93"><event><die/></event><conditions><status-is status="START"/><variable-at-least field="step" value="93"/><variable-below field="step" value="99"/></conditions></transition>
						  </transitions>
						</quest-definition>

				""";

		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition wildcard = compiled.definition().transitions().get(1);
		assertEquals(null, wildcard.sourceNode());
		int packed = compiled.definition().progressLayout().pack(Map.of("step", 94));
		assertTrue(QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, 20044, QuestStatus.START, packed, Map.of()),
			new QuestEvent.Die(), wildcard).isPresent());
	}

	@Test
	void compilesAndEvaluatesCompleteCountCondition() {
		String xml = """

						<quest-definition id="20046" version="1">
						  <metadata name="complete-count" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><bonus-apply bonus-type="MOVIE"/></event>
						    <conditions><complete-count-is value="9"/></conditions>
						    <actions><grant-reward kind="ITEM" id="188051106" amount="1"/></actions>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.CompleteCountIs(9)), transition.conditions());
		assertEquals(List.of(new QuestAction.GrantReward("ITEM", 188051106, 1)), transition.actions());

		var layout = compiled.definition().progressLayout();
		var ninth = new QuestSnapshot(7, 20046, QuestStatus.REWARD, 0, Map.of()).withCompleteCount(9);
		var eighth = ninth.withCompleteCount(8);
		assertTrue(QuestConditionEvaluator.matches(layout, ninth, List.of(new QuestCondition.CompleteCountIs(9))));
		assertFalse(QuestConditionEvaluator.matches(layout, eighth, List.of(new QuestCondition.CompleteCountIs(9))));
		assertTrue(QuestConditionEvaluator.matches(layout, eighth,
			List.of(new QuestCondition.CompleteCountIs(9, false))));
	}

	@Test
	void compilesAndEvaluatesEventActiveConditionWithAbandonQuestAction() {
		String xml = """

						<quest-definition id="20047" version="1">
						  <metadata name="event-abandon" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes>
						    <node label="started" status="START"/>
						    <node label="abandoned" status="NONE"/>
						  </nodes>
						  <transitions><transition source="started" target="abandoned">
						    <event><level-up/></event>
						    <conditions><event-active expected="false"/></conditions>
						    <actions><abandon-quest/></actions>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		QuestTransition transition = compiled.definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.EventActive(false)), transition.conditions());
		assertEquals(List.of(new QuestAction.AbandonQuest()), transition.actions());

		var layout = compiled.definition().progressLayout();
		var expired = new QuestSnapshot(7, 20047, QuestStatus.START, 0, Map.of()).withEventActive(false);
		var active = expired.withEventActive(true);
		var uncaptured = new QuestSnapshot(7, 20047, QuestStatus.START, 0, Map.of());
		assertTrue(QuestConditionEvaluator.matches(layout, expired, transition.conditions()));
		assertFalse(QuestConditionEvaluator.matches(layout, active, transition.conditions()));
		assertFalse(QuestConditionEvaluator.matches(layout, uncaptured, transition.conditions()));
	}

	@Test
	void compilesRandomMovieAction() {
		String xml = """

						<quest-definition id="20048" version="1">
						  <metadata name="random-movie" display-name-id="0" min-level="1" max-level="55" category="QUEST"/>
						  <nodes><node label="started" status="START"/></nodes>
						  <transitions><transition source="started" target="started">
						    <event><bonus-apply bonus-type="MOVIE"/></event>
						    <after-commit><play-movie-random><variant movie-id="103"/><variant movie-id="104"/></play-movie-random></after-commit>
						  </transition></transitions>
						</quest-definition>

				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		AfterCommitAction.PlayMovieRandom random = assertInstanceOf(AfterCommitAction.PlayMovieRandom.class,
			compiled.definition().transitions().get(0).afterCommit().get(0));
		assertEquals(List.of(103, 104), random.movieIds());

		String singleVariant = xml.replace("movie-id=\"104\"/>", "");
		assertThrows(QuestCompilationException.class, () -> QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(singleVariant.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	void dslLowersEventAbandonAndRandomMovieCapabilities() {
		CompiledQuestDefinition abandon = QuestDsl.quest(20049)
			.node("started", QuestDsl.project(QuestStatus.START, Map.of()))
			.node("abandoned", QuestDsl.project(QuestStatus.NONE, Map.of()))
			.on(QuestDsl.levelUp()).when(QuestDsl.eventActive(false)).then(QuestDsl.abandonQuest())
			.from("started").goTo("abandoned")
			.compile();
		QuestTransition abandonedTransition = abandon.definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.EventActive(false)), abandonedTransition.conditions());
		assertEquals(List.of(new QuestAction.AbandonQuest()), abandonedTransition.actions());

		CompiledQuestDefinition movie = QuestDsl.quest(20050)
			.node("started", QuestDsl.project(QuestStatus.START, Map.of()))
			.on(QuestDsl.bonusApply("MOVIE")).when(QuestDsl.completeCountIs(9))
			.then(QuestDsl.grantReward("ITEM", 188051106, 1)).goTo("started")
			.afterCommit(QuestDsl.playMovieRandom(103, 104))
			.compile();
		QuestTransition movieTransition = movie.definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.CompleteCountIs(9)), movieTransition.conditions());
		assertEquals(new AfterCommitAction.PlayMovieRandom(List.of(103, 104)),
			movieTransition.afterCommit().get(0));
	}
}
