package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeAllItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.worldNpcIs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMutationPlannerTest {
	private static final int QUEST_ID = 1300;
	private static final int ITEM_ID = 182400001;

	@Test
	void removeAllUsesCapturedInventoryFactsWithoutComparingAgainstSentinel() {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0,
			Map.of(ITEM_ID, 4));

		assertTrue(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void removeAllFailsClosedWhenInventoryFactsAreUnknown() {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0,
			null, Map.of(), false, true, 0);

		assertFalse(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void worldNpcConditionUsesCapturedCurrentInstanceFactsAndFailsClosedWhenUnknown() {
		CompiledQuestDefinition definition = QuestDsl.quest(QUEST_ID + 1)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.TalkToNpc(700243)).from("started")
			.when(worldNpcIs(212814, false)).goTo("reward")
			.compile();
		QuestSnapshot absent = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(java.util.Set.of()));
		QuestSnapshot present = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(java.util.Set.of(212814)));
		QuestSnapshot unknown = new QuestSnapshot(7, QUEST_ID + 1, QuestStatus.START, 0, Map.of());
		var transition = definition.definition().transitions().get(0);

		assertTrue(QuestMutationPlanner.plan(definition, absent,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, present,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, unknown,
			new QuestEvent.TalkToNpc(700243), transition).isPresent());
	}

	@Test
	void zoneConditionUsesCapturedCurrentZoneFactsAndFailsClosedWhenUnknown() {
		CompiledQuestDefinition definition = QuestDsl.quest(QUEST_ID + 2)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.UseItem(182400002)).from("started")
			.when(QuestDsl.zoneIs("BERITRAS_WEAPON_220040000"))
			.goTo("reward")
			.compile();
		var transition = definition.definition().transitions().get(0);
		QuestSnapshot inside = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of("BERITRAS_WEAPON_220040000")));
		QuestSnapshot outside = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of())
			.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of("BELUSLAN_FORTRESS_220040000")));
		QuestSnapshot unknown = new QuestSnapshot(7, QUEST_ID + 2, QuestStatus.START, 0, Map.of());

		assertTrue(QuestMutationPlanner.plan(definition, inside,
			new QuestEvent.UseItem(182400002), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, outside,
			new QuestEvent.UseItem(182400002), transition).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, unknown,
			new QuestEvent.UseItem(182400002), transition).isPresent());
	}

	@Test
	void beautifulFeatherCleansEveryWorkItemOnRewardRoutes() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/2392.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}
		// 真端 quest.xml: quest_2392a/b/c 各 1 个；remove 显式 count=1 与收集数量一致（404c5814b 起 XML 用精确数量而非 ALL）
		List<QuestAction> cleanup = List.of(
			new QuestAction.RemoveItem(182204159, 1),
			new QuestAction.RemoveItem(182204160, 1),
			new QuestAction.RemoveItem(182204161, 1));
		var routes = definition.definition().transitions().stream()
			.filter(transition -> Set.of("r1", "r2", "r3").contains(transition.sourceNode()))
			.filter(transition -> transition.targetNode().equals("complete")
				|| transition.targetNode().equals(transition.sourceNode()))
			.toList();

		assertTrue(routes.size() >= 6);
		assertTrue(routes.stream().allMatch(route -> route.actions().containsAll(cleanup)));
	}

	@Test
	void ringForLuckRemovesItsQuestWorkItemWhenCompleting() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/2578.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}

		var completion = definition.definition().transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();

		assertTrue(completion.actions().contains(
			new QuestAction.RemoveItem(182204453, QuestAction.RemoveItem.ALL)));
	}

	@Test
	void abandonQuestLowersQuestWorkItemsToTransactionalRemoveAllActions() {
		String xml = """
				<quest-definition id="1302" version="1">
				  <metadata name="abandon" display-name-id="0" min-level="1" max-level="55" category="QUEST">
				    <work-items><item id="182400003" count="1"/></work-items>
				  </metadata>
				  <nodes>
				    <node label="started"><project status="START"/></node>
				    <node label="unaccepted"><project status="NONE"/></node>
				  </nodes>
				  <transitions>
				    <transition source="started" target="unaccepted">
				      <event><abandon/></event>
				      <actions><abandon-quest/></actions>
				    </transition>
				  </transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		var transition = definition.definition().transitions().get(0);
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1302, QuestStatus.START, 0, Map.of(182400003, 2)),
			new QuestEvent.Abandon(), transition).orElseThrow();

		assertEquals(List.of(new QuestAction.AbandonQuest(),
			new QuestAction.RemoveItem(182400003, QuestAction.RemoveItem.ALL)), plan.requiredActions());
	}

	@Test
	void lowersSelectedMetadataRewardToQuestBaseCurrencyReward() {
		String xml = """
				<quest-definition id="1303" version="1">
				  <metadata name="selected" display-name-id="0" min-level="1" max-level="55" category="QUEST">
				    <rewards><reward kind="AP" id="0" amount="300"/><reward kind="AP" id="0" amount="600"/></rewards>
				  </metadata>
				  <nodes><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="started" target="started">
				    <event><talk-to-npc npc-id="700001"/></event>
				    <actions><grant-selected-reward reward-index="1"/></actions>
				  </transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		var transition = definition.definition().transitions().get(0);
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1303, QuestStatus.START, 0, Map.of(), Map.of()),
			new QuestEvent.TalkToNpc(700001), transition).orElseThrow();

		assertEquals(List.of(new QuestAction.GrantReward("AP", 0, 600,
			com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode.QUEST_BASE)),
			plan.requiredActions());
	}

	@Test
	void startConditionGroupsAreAndInsideAndOrAcrossGroups() {
		String xml = """
				<quest-definition id="1306" version="1">
				  <metadata name="grouped-start" display-name-id="0" min-level="1" max-level="55" category="QUEST">
				    <start-condition-groups>
				      <group><condition type="finished" quest-id="9001"/><condition type="acquired" quest-id="9002"/></group>
				      <group><condition type="finished" quest-id="9003"/></group>
				    </start-condition-groups>
				  </metadata>
				  <nodes><node label="none"><project status="NONE"/></node><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="none" target="started"><event><talk-to-npc npc-id="700001"/></event></transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(
			new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		var transition = definition.definition().transitions().get(0);
		QuestEvent event = new QuestEvent.TalkToNpc(700001);

		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1306, QuestStatus.NONE, 0, Map.of()).withCompletedQuestIds(Set.of(9003)),
			event, transition).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1306, QuestStatus.NONE, 0, Map.of())
				.withCompletedQuestIds(Set.of(9001)).withActiveQuestIds(Set.of(9002)),
			event, transition).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1306, QuestStatus.NONE, 0, Map.of()).withCompletedQuestIds(Set.of(9001)),
			event, transition).isEmpty());
	}

	@Test
	void unequipRemoveCountIsConsumedBeforeASeparateInventoryRemoval() {
		CompiledQuestDefinition definition = QuestDsl.quest(1304)
			.node("started", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(new QuestEvent.TalkToNpc(700001)).from("started")
			.then(new QuestAction.UnequipItem(ITEM_ID, 1))
			.then(new QuestAction.RemoveItem(ITEM_ID, 1))
			.goTo("reward")
			.compile();
		var snapshot = new QuestSnapshot(7, 1304, QuestStatus.START, 0, Map.of())
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(), Map.of(ITEM_ID, 1)));

		assertTrue(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isEmpty());
	}

	@Test
	void repeatedInventoryRemovalsAreValidatedCumulatively() {
		CompiledQuestDefinition definition = QuestDsl.quest(1305)
			.node("started", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(new QuestEvent.TalkToNpc(700001)).from("started")
			.then(new QuestAction.RemoveItem(ITEM_ID, 1))
			.then(new QuestAction.RemoveItem(ITEM_ID, 1))
			.goTo("reward")
			.compile();

		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1305, QuestStatus.START, 0, Map.of(ITEM_ID, 1)),
			new QuestEvent.TalkToNpc(700001), definition.definition().transitions().get(0)).isEmpty());
	}

	@Test
	void npcFactionLifecycleIsScheduledAroundTypedQuestStateTransitions() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/36539.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}

		var accept = definition.definition().transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		var acceptPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 36539, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()),
			new QuestEvent.TalkToNpc(804952, 1002), accept).orElseThrow();
		assertTrue(acceptPlan.afterCommit().get(0) instanceof AfterCommitAction.StartNpcFactionQuest);
		assertEquals(4, ((AfterCommitAction.StartNpcFactionQuest) acceptPlan.afterCommit().get(0)).npcFactionId());

		var completion = definition.definition().transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		var completionPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 36539, QuestStatus.REWARD, 1, Map.of()),
			new QuestEvent.TalkToNpc(804952, 8), completion).orElseThrow();
		assertTrue(completionPlan.afterCommit().get(0) instanceof AfterCommitAction.CompleteNpcFactionQuest);
		assertEquals(4, ((AfterCommitAction.CompleteNpcFactionQuest) completionPlan.afterCommit().get(0)).npcFactionId());
	}

	@Test
	void dailyRotatingNpcFactionQuestStartsTheFactionLifecycleOnAccept() throws Exception {
		// 真端依据:36525 阵营任务每日轮换,daily 标志不可靠;NONE→START 接取即应
		// 启动阵营生命周期,不再按 timeBased 取消。
		CompiledQuestDefinition definition;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/36525.xml"))) {
			definition = QuestDefinitionXmlCompiler.compile(input);
		}

		var accept = definition.definition().transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		var acceptPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 36525, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()),
			new QuestEvent.TalkToNpc(799837, 1002), accept).orElseThrow();

		assertTrue(acceptPlan.afterCommit().stream()
			.anyMatch(AfterCommitAction.StartNpcFactionQuest.class::isInstance));
	}

	private static CompiledQuestDefinition definition() {
		return QuestDsl.quest(QUEST_ID)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.TalkToNpc(700001)).from("started")
			.then(removeAllItem(ITEM_ID)).goTo("reward")
			.compile();
	}
}
