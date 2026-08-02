package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestCraftSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.abandon;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.canGrantCraftSkill;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.completeQuest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.failCraft;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.forgetRecipe;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.grantCraftSkill;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.grantReward;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.learnRecipe;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.movieEnd;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.playMovie;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.recipeNotKnown;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CRAFT vertical proofs extracted from real owners 19038, 5000 and 1941. */
class CraftRepresentativeQuestDefinitionTest {
	private static final EvidenceRef FAIL_CRAFT_EVIDENCE = new EvidenceRef("HANDLER",
		"src/main/java/com/aionemu/gameserver/quest/handlers/crafting/_19038MasterCooksPotential.java:180",
		"onFailCraftEvent rolls var0 2/5/8/11 back to 1/4/7/10 for four exact item ids");
	private static final EvidenceRef WORK_ORDER_EVIDENCE = new EvidenceRef("XML_TEMPLATE",
		"src/main/resources/aion/data/static_data/quest_script_data/work_order.xml:8",
		"work_order 5000 learns recipe 155004001 on accept and releases it on completion or abandon");
	private static final EvidenceRef CRAFT_REWARD_EVIDENCE = new EvidenceRef("XML_TEMPLATE",
		"src/main/resources/aion/data/static_data/quest_script_data/crafting.xml:9",
		"crafting_rewards 1941 plays movie 93 then grants skill 40002 level 400 with auto recipes");

	@Test
	void failCraft19038ProjectsAllFourExactRollbacks() {
		assertFailCraftRollback(182206773, 2, 1);
		assertFailCraftRollback(182206774, 5, 4);
		assertFailCraftRollback(182206775, 8, 7);
		assertFailCraftRollback(182206776, 11, 10);
	}

	@Test
	void workOrder5000DslAndXmlCompileToSameLifecycleSafeIr() {
		CompiledQuestDefinition dsl = workOrder5000Dsl();
		CompiledQuestDefinition xml = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			workOrder5000Xml().getBytes(StandardCharsets.UTF_8)));

		assertEquals(dsl.definition(), xml.definition());
		assertEquals(List.of(
			new QuestAction.GrantReward("ITEM", 182290000, 4),
			new QuestAction.LearnRecipe(155004001, QuestRecipeOwnership.QUEST_OWNED)),
			dsl.definition().transitions().get(0).actions());
		assertEquals(new QuestEvent.Abandon(), dsl.definition().transitions().get(1).event());
		assertEquals(List.of(new QuestAction.ForgetRecipe(155004001)),
			dsl.definition().transitions().get(1).actions());
		assertEquals(List.of(new QuestAction.ForgetRecipe(155004001), new QuestAction.CompleteQuest(0)),
			dsl.definition().transitions().get(2).actions());
	}

	@Test
	void questOwnedRecipeWithoutAbandonCleanupFailsCompilation() {
		QuestCompilationException failure = assertThrows(QuestCompilationException.class, () -> quest(5000)
			.evidence(WORK_ORDER_EVIDENCE)
			.metadata(QuestMetadata.minimal("Steel Chisel Supplies", 1190000, "TASK"))
			.node("none", project(QuestStatus.NONE, Map.of()))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()))
			.on(talkToNpc(203788, QuestDialog.ACCEPT_QUEST)).from("none")
			.then(learnRecipe(155004001, QuestRecipeOwnership.QUEST_OWNED)).goTo("start")
			.on(talkToNpc(203788, QuestDialog.SELECT_REWARD)).from("start")
			.then(forgetRecipe(155004001)).then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION))
			.compile());

		assertEquals("CRAFT_LIFECYCLE_INCOMPLETE", failure.code());
	}

	@Test
	void craftingReward1941UsesMovieCallbackForSkillAndAutoRecipes() {
		CompiledQuestDefinition definition = quest(1941)
			.evidence(CRAFT_REWARD_EVIDENCE)
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(talkToNpc(203700, QuestDialog.SELECT_REWARD)).from("start")
			.when(canGrantCraftSkill(40002, 400)).goTo("reward")
			.afterCommit(playMovie(93))
			.on(movieEnd(93)).from("reward").when(canGrantCraftSkill(40002, 400))
			.then(grantCraftSkill(40002, 400, true)).goTo("reward")
			.compile();
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
			craftReward1941Xml().getBytes(StandardCharsets.UTF_8)));

		assertEquals(definition.definition(), fromXml.definition());
		assertEquals(List.of(new AfterCommitAction.PlayMovie(93)),
			definition.definition().transitions().get(0).afterCommit());
		assertEquals(List.of(new QuestAction.GrantCraftSkill(40002, 400, true)),
			definition.definition().transitions().get(1).actions());
		QuestSnapshot eligible = new QuestSnapshot(7, 1941, QuestStatus.REWARD, 0, Map.of())
			.withCraftFacts(new QuestCraftSnapshot(Set.of(), Map.of(40002, 399), 1600, 2, 1));
		assertEquals(400,
			((QuestAction.GrantCraftSkill) QuestMutationPlanner.plan(definition, eligible,
				movieEnd(93), definition.definition().transitions().get(1)).orElseThrow()
				.requiredActions().get(0)).targetLevel());
	}

	@Test
	void craftConditionsFailClosedWhenFactsWereNotCaptured() {
		CompiledQuestDefinition definition = workOrder5000Dsl();
		QuestSnapshot unknown = new QuestSnapshot(7, 5000, QuestStatus.NONE, 0, Map.of());

		assertFalse(QuestMutationPlanner.plan(definition, unknown,
			talkToNpc(203788, QuestDialog.ACCEPT_QUEST), definition.definition().transitions().get(0)).isPresent());
	}

	private static void assertFailCraftRollback(int itemId, int from, int to) {
		CompiledQuestDefinition definition = quest(19038)
			.evidence(FAIL_CRAFT_EVIDENCE)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("failed", project(QuestStatus.START, vars("var0", from)))
			.node("retry", project(QuestStatus.START, vars("var0", to)))
			.on(failCraft(itemId)).from("failed").goTo("retry")
			.compile();
		QuestSnapshot snapshot = new QuestSnapshot(7, 19038, QuestStatus.START, from, Map.of());

		assertEquals(to, QuestMutationPlanner.plan(definition, snapshot, failCraft(itemId),
			definition.definition().transitions().get(0)).orElseThrow().nextPackedVariables());
	}

	private static CompiledQuestDefinition workOrder5000Dsl() {
		return quest(5000)
			.evidence(WORK_ORDER_EVIDENCE)
			.metadata(QuestMetadata.minimal("Steel Chisel Supplies", 1190000, "TASK"))
			.node("none", project(QuestStatus.NONE, Map.of()))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("complete", project(QuestStatus.COMPLETE, Map.of()))
			.on(talkToNpc(203788, QuestDialog.ACCEPT_QUEST)).from("none")
			.when(recipeNotKnown(155004001))
			.then(grantReward("ITEM", 182290000, 4))
			.then(learnRecipe(155004001, QuestRecipeOwnership.QUEST_OWNED)).goTo("start")
			.on(abandon()).from("start").then(forgetRecipe(155004001)).goTo("none")
			.on(talkToNpc(203788, QuestDialog.SELECT_REWARD)).from("start")
			.then(forgetRecipe(155004001)).then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION))
			.compile();
	}

	private static String workOrder5000Xml() {
		return """
			<quest-definition id="5000" version="1" ownership="COMPILED_CANDIDATE">
			  <evidence><ref source="XML_TEMPLATE" locator="src/main/resources/aion/data/static_data/quest_script_data/work_order.xml:8" statement="work_order 5000 learns recipe 155004001 on accept and releases it on completion or abandon"/></evidence>
			  <metadata name="Steel Chisel Supplies" display-name-id="1190000" min-level="0" max-level="2147483647" category="TASK"/>
			  <nodes>
			    <node label="none"><project status="NONE"/></node>
			    <node label="start"><project status="START"/></node>
			    <node label="complete"><project status="COMPLETE"/></node>
			  </nodes>
			  <transitions>
			    <transition source="none" target="start"><event><talk-to-npc npc-id="203788" dialog="ACCEPT_QUEST"/></event>
			      <conditions><recipe-known recipe-id="155004001" expected="false"/></conditions>
			      <actions><grant-reward kind="ITEM" id="182290000" amount="4"/><learn-recipe recipe-id="155004001" ownership="QUEST_OWNED"/></actions>
			    </transition>
			    <transition source="start" target="none"><event><abandon/></event><actions><forget-recipe recipe-id="155004001"/></actions></transition>
			    <transition source="start" target="complete"><event><talk-to-npc npc-id="203788" dialog="SELECT_REWARD"/></event><actions><forget-recipe recipe-id="155004001"/><complete-quest reward-index="0"/></actions><after-commit><sync-quest-state mode="COMPLETION"/></after-commit></transition>
			  </transitions>
			</quest-definition>
			""";
	}

	private static String craftReward1941Xml() {
		return """
			<quest-definition id="1941" version="1" ownership="COMPILED_CANDIDATE">
			  <evidence><ref source="XML_TEMPLATE" locator="src/main/resources/aion/data/static_data/quest_script_data/crafting.xml:9" statement="crafting_rewards 1941 plays movie 93 then grants skill 40002 level 400 with auto recipes"/></evidence>
			  <metadata name="quest-1941" display-name-id="0" min-level="0" max-level="2147483647" category="QUEST"/>
			  <nodes><node label="start"><project status="START"/></node><node label="reward"><project status="REWARD"/></node></nodes>
			  <transitions>
			    <transition source="start" target="reward"><event><talk-to-npc npc-id="203700" dialog="SELECT_REWARD"/></event>
			      <conditions><can-grant-craft-skill skill-id="40002" target-level="400"/></conditions>
			      <after-commit><play-movie movie-id="93"/></after-commit>
			    </transition>
			    <transition source="reward" target="reward"><event><movie-end movie-id="93"/></event>
			      <conditions><can-grant-craft-skill skill-id="40002" target-level="400"/></conditions>
			      <actions><grant-craft-skill skill-id="40002" target-level="400" auto-learn-recipes="true"/></actions>
			    </transition>
			  </transitions>
			</quest-definition>
			""";
	}
}
