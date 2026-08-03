package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.addAggroList;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bastionReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.dredgionReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.enterWindStream;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.houseItemUse;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.kamarReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.logOut;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.ophidanReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.passFlyingRing;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.useSkill;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real handler-backed representative events plus explicitly non-representative typed variants. */
class RemainingCapabilityDefinitionTest {
	@Test
	void currentHandlerBackedEventsCompileThroughDslAndXml() {
		assertEquivalent(14211, addAggroList(277224), "<add-aggro-list npc-id=\"277224\"/>");
		assertEquivalent(14212, QuestDsl.questDialog(QuestDialog.ACCEPT_QUEST),
			"<quest-dialog dialog=\"ACCEPT_QUEST\"/>");
		assertEquivalent(18830, houseItemUse(3420021), "<house-item-use item-id=\"3420021\"/>");
		assertEquivalent(1354, passFlyingRing("ERACUS_TEMPLE_AIR_BOOSTER_1"),
			"<pass-flying-ring ring=\"ERACUS_TEMPLE_AIR_BOOSTER_1\"/>");
		assertEquivalent(11076, enterWindStream(405001), "<enter-wind-stream teleport-id=\"405001\"/>");
		assertEquivalent(3718, dredgionReward(), "<dredgion-reward/>");
		assertEquivalent(3050, logOut(), "<log-out/>");
		assertEquivalent(11468, useSkill(9832), "<use-skill skill-id=\"9832\"/>");
	}

	@Test
	void instanceRewardVariantsWithoutCurrentHandlerOwnersRemainTypedFixtures() {
		assertEquivalent(90001, kamarReward(), "<kamar-reward/>");
		assertEquivalent(90002, ophidanReward(), "<ophidan-reward/>");
		assertEquivalent(90003, bastionReward(), "<bastion-reward/>");
	}

	@Test
	void runtimeFactsMatchDefinitionEventsWithoutPersistingFacts() {
		QuestAiPerceptionFacts ai = new QuestAiPerceptionFacts(7, 20, 277224, 20, true, true,
			210130000, 210130000, 1, 1, 10d, 50, true, true);
		QuestMovementFacts movement = new QuestMovementFacts(7, 210130000, 1, 1f, 2f, 3f, true, true, "405001");
		QuestSkillFacts skill = new QuestSkillFacts(7, 9832, 20, 277224, 0, 210130000, 1, true);
		assertTrue(QuestEvent.matches(new QuestEvent.AddAggroList(277224), new QuestEvent.AddAggroList(277224, ai)));
		assertTrue(QuestEvent.matches(new QuestEvent.EnterWindStream(405001), new QuestEvent.EnterWindStream(405001, movement)));
		assertTrue(QuestEvent.matches(new QuestEvent.UseSkill(9832), new QuestEvent.UseSkill(9832, skill)));
		assertTrue(QuestEvent.matches(new QuestEvent.UseItem(182200501),
			new QuestEvent.UseItem(182200501, 900008)));
		assertTrue(QuestEvent.matches(new QuestEvent.QuestDialog(QuestDialog.ACCEPT_QUEST.id()),
			new QuestEvent.QuestDialog(QuestDialog.ACCEPT_QUEST.id())));
		assertEquals(new QuestEvent.UseItem(182200501),
			QuestEvent.routeKey(new QuestEvent.UseItem(182200501, 900008)));
	}

	@Test
	void xmlAndDslCompileTheNewTypedActionsAndRemoveAll() {
		CompiledQuestDefinition dsl = quest(14213)
			.metadata(QuestMetadata.minimal("typed-actions", 1, "QUEST"))
			.progress(QuestDsl.bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(QuestDsl.questDialog(QuestDialog.ACCEPT_QUEST)).from("start")
			.when(statusIs(QuestStatus.START))
			.then(QuestDsl.removeAllItem(182200501))
			.then(QuestDsl.setVariable("var0", 1))
			.goTo("done")
			.afterCommit(QuestDsl.playerEmotion(QuestPlayerEmotion.STAND))
			.afterCommit(QuestDsl.addNpcAggro(203175, 50))
			.compile();
		String xml = """
			<quest-definition id="14213" version="1">
			  <metadata name="typed-actions" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes><node label="start"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
			    <node label="done"><project status="REWARD"><vars><var name="var0" value="1"/></vars></project></node></nodes>
			  <transitions><transition source="start" target="done"><event><quest-dialog dialog="ACCEPT_QUEST"/></event>
			    <conditions><status-is status="START"/></conditions>
			    <actions><remove-item item-id="182200501" count="ALL"/><set-variable field="var0" value="1"/></actions>
			    <after-commit><player-emotion emotion="STAND"/><add-npc-aggro npc-id="203175" damage="50"/></after-commit>
			  </transition></transitions>
			</quest-definition>
			""";
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

		assertEquals(dsl.definition(), fromXml.definition());
	}

	private static void assertEquivalent(int id, QuestEvent event, String xmlEvent) {
		CompiledQuestDefinition dsl = quest(id)
			.metadata(QuestMetadata.minimal("representative", 1, "QUEST"))
			.progress(QuestDsl.bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(event).from("start").when(statusIs(QuestStatus.START))
			.then(QuestDsl.setVariable("var0", 1)).goTo("done").compile();
		String xml = """
			<quest-definition id="%d" version="1">
			  <metadata name="representative" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes><node label="start"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
			    <node label="done"><project status="REWARD"><vars><var name="var0" value="1"/></vars></project></node></nodes>
			  <transitions><transition source="start" target="done"><event>%s</event><conditions><status-is status="START"/></conditions><actions><set-variable field="var0" value="1"/></actions></transition></transitions>
			</quest-definition>
			""".formatted(id, xmlEvent);
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(dsl.definition(), fromXml.definition());
		assertEquals(List.of(event.type()), fromXml.transitionsByType().keySet().stream().toList());
		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, id, QuestStatus.START, 0, Map.of()), event,
			dsl.definition().transitions().getFirst());
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.REWARD, plan.orElseThrow().nextStatus());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
	}

}
