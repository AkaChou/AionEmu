package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LegacyKillFlowRepairDefinitionTest {
	@Test
	void killMilestonesRemainStartedUntilTheLegacyHandoff() {
		for (int questId : List.of(2620, 4210, 18302, 18303, 18510, 23702, 23703, 23705)) {
			CompiledQuestDefinition definition = load(questId);
			assertFalse(definition.definition().transitions().stream()
				.anyMatch(transition -> isKill(transition.event()) && transition.targetNode().equals("reward")),
				() -> questId + " must not become reward-ready directly from its ordinary kill milestone");
		}

		QuestTransition phagrasulReport = talk(load(2620), "s1", 204787, 1009, "reward");
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var1", 5)));
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var2", 5)));

		assertEquals("reward", talk(load(18302), "started", 730375, 10255, "reward").targetNode());
		assertEquals("reward", talk(load(18303), "started", 700980, -1, "reward").targetNode());
		assertEquals("reward", talk(load(23702), "started", 802354, 1009, "reward").targetNode());
		assertEquals("reward", talk(load(23703), "started", 802353, 1009, "reward").targetNode());

		QuestTransition oldRoadGate = talk(load(23705), "started", 802345, 1009, "reward");
		assertTrue(oldRoadGate.conditions().contains(new QuestCondition.QuestVariableIs("var0", 3)));
		assertFalse(oldRoadGate.conditions().stream().anyMatch(condition ->
			condition instanceof QuestCondition.QuestVariableIs variable && variable.field().equals("var1")));
	}

	@Test
	void independentLegacyKillBitsRemainIndependent() {
		CompiledQuestDefinition missingHaorunerk = load(4210);
		assertEquals(Set.of("var1", "var2"), fieldNames(missingHaorunerk, "var1", "var2"));
		assertSetsBit(kill(missingHaorunerk, "s1", 215056), "var1");
		assertSetsBit(kill(missingHaorunerk, "s1", 215080), "var2");

		CompiledQuestDefinition fate = load(4502);
		assertEquals(Set.of("var1", "var2", "var3"), fieldNames(fate, "var1", "var2", "var3"));
		assertSetsBit(kill(fate, "s2", 214895), "var1");
		assertSetsBit(kill(fate, "s2", 214896), "var2");
		assertSetsBit(kill(fate, "s2", 214897), "var3");
		QuestTransition itemReport = talk(fate, "s2", 204837, 39, "reward");
		assertTrue(itemReport.conditions().contains(new QuestCondition.HasItem(182204534, 1)));
		assertTrue(itemReport.actions().contains(new QuestAction.RemoveItem(182204534, 1)));

		CompiledQuestDefinition destroyingWeapons = load(2633);
		assertEquals("s2", talk(destroyingWeapons, "s1", 700296, -1, "s2").targetNode());
		assertEquals("reward", kill(destroyingWeapons, "s2", 213933).targetNode());
	}

	@Test
	void missionKillChainsPreserveTheirDialogZoneAndItemStages() {
		CompiledQuestDefinition totem = load(24015);
		assertEquals("s2", totem.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s1")
				&& transition.event().equals(new QuestEvent.EnterZone("BLACK_CLAW_OUTPOST_220030000")))
			.findFirst().orElseThrow().targetNode());
		assertEquals(List.of("s3", "s4", "reward"), totem.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(700099)))
			.map(QuestTransition::targetNode).toList());
		assertTrue(totem.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete"))
			.allMatch(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId() == 203557));

		CompiledQuestDefinition frozenCity = load(24052);
		List<QuestTransition> itemUses = frozenCity.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.UseItem).toList();
		assertEquals(9, itemUses.size());
		assertTrue(itemUses.stream().allMatch(transition ->
			transition.conditions().contains(new QuestCondition.ZoneIs("DF3_ITEMUSEAREA_Q2056"))
				&& transition.actions().contains(new QuestAction.BlockDefaultItemUse())));
		assertEquals(3, itemUses.stream().filter(transition -> transition.targetNode().equals("s4")
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.SpawnNpc.class::isInstance)
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.StartQuestTimer.class::isInstance)).count());
		assertEquals("reward", kill(frozenCity, "s4", 233864).targetNode());

		CompiledQuestDefinition crisis = load(24054);
		assertEquals("s5", kill(crisis, "s2", 702041).targetNode());
		assertEquals("s6", kill(crisis, "s5", 233865).targetNode());
		assertEquals("reward", talk(crisis, "s6", 204701, 10255, "reward").targetNode());

		CompiledQuestDefinition umkata = load(24114);
		QuestTransition spirit = umkata.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet set
				&& set.npcIds().equals(Set.of(210722, 210588))).findFirst().orElseThrow();
		assertEquals("started", spirit.targetNode());
		QuestTransition boss = kill(umkata, "started", 210752);
		assertEquals("reward", boss.targetNode());
		assertEquals(Set.of(182215474, 182215475, 182215476), boss.actions().stream()
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(QuestAction.RemoveItem.class::cast).map(QuestAction.RemoveItem::itemId)
			.collect(Collectors.toSet()));
	}

	@Test
	void daevanionKillStagesUseClientCountsAndResetTheSharedPackedCounter() {
		CompiledQuestDefinition pants = load(15304);
		QuestSnapshot pantsState = snapshot(pants, 2, 0, Map.of(182215835, 1));
		pantsState = applyKills(pants, pantsState, 883285, 59);
		assertProjection(pants, pantsState, QuestStatus.START, 2, 59);
		pantsState = apply(pants, pantsState, new QuestEvent.KillNpc(883285));
		assertProjection(pants, pantsState, QuestStatus.START, 3, 0);
		pantsState = apply(pants, pantsState, new QuestEvent.TalkToNpc(805328, 10255));
		assertProjection(pants, pantsState, QuestStatus.REWARD, 4, 0);

		CompiledQuestDefinition weapon = load(15306);
		assertEquals(Set.of("var0", "var1"), weapon.definition().progressLayout().fields().stream()
			.map(BitField::name).collect(Collectors.toSet()));
		QuestSnapshot state = snapshot(weapon, 3, 0, Map.of());
		state = applyKills(weapon, state, 233941, 59);
		assertProjection(weapon, state, QuestStatus.START, 3, 59);
		state = applyKills(weapon, state, 233941, 1);
		assertProjection(weapon, state, QuestStatus.START, 4, 0);
		state = applyKills(weapon, state, 234292, 60);
		assertProjection(weapon, state, QuestStatus.START, 5, 0);
		state = applyKills(weapon, state, 883308, 30);
		assertProjection(weapon, state, QuestStatus.START, 6, 0);
		state = applyKills(weapon, state, 232853, 1);
		assertProjection(weapon, state, QuestStatus.START, 7, 0);
		state = applyKills(weapon, state, 236277, 5);
		assertProjection(weapon, state, QuestStatus.START, 8, 0);
		state = apply(weapon, state, new QuestEvent.TalkToNpc(805328, 10008));
		assertProjection(weapon, state, QuestStatus.START, 9, 0);
		state = apply(weapon, state, new QuestEvent.TalkToNpc(805327, 1009));
		assertProjection(weapon, state, QuestStatus.REWARD, 10, 0);
	}

	private static Set<String> fieldNames(CompiledQuestDefinition definition, String... names) {
		return java.util.Arrays.stream(names)
			.filter(name -> definition.definition().progressLayout().field(name) != null)
			.collect(Collectors.toSet());
	}

	private static void assertSetsBit(QuestTransition transition, String field) {
		assertTrue(transition.actions().contains(new QuestAction.SetVariable(field, 1)));
	}

	private static boolean isKill(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	private static QuestTransition kill(CompiledQuestDefinition definition, String source, int npcId) {
		return transition(definition, source, transition -> QuestEvent.matches(transition.event(), new QuestEvent.KillNpc(npcId)));
	}

	private static QuestTransition talk(CompiledQuestDefinition definition, String source, int npcId,
			int dialogId, String target) {
		return transition(definition, source, transition -> transition.targetNode().equals(target)
			&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)));
	}

	private static QuestTransition transition(CompiledQuestDefinition definition, String source,
			Predicate<QuestTransition> predicate) {
		return definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source) && predicate.test(transition))
			.findFirst().orElseThrow();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, int var0, int var1,
			Map<Integer, Integer> inventory) {
		int packed = definition.definition().progressLayout().pack(Map.of("var0", var0, "var1", var1));
		return new QuestSnapshot(7, definition.id(), QuestStatus.START, packed, inventory);
	}

	private static QuestSnapshot applyKills(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			int npcId, int count) {
		QuestSnapshot current = snapshot;
		for (int i = 0; i < count; i++) {
			current = apply(definition, current, new QuestEvent.KillNpc(npcId));
		}
		return current;
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestTransition> candidates = definition.transitionsFor(event.type()).stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.sorted(Comparator.comparingInt(transition -> transition.priority() == null
				? Integer.MAX_VALUE : transition.priority()))
			.toList();
		for (QuestTransition transition : candidates) {
			var plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				QuestMutationPlan mutation = plan.orElseThrow();
				return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), mutation.nextStatus(),
					mutation.nextPackedVariables(), snapshot.inventory());
			}
		}
		return fail("no eligible transition for quest " + definition.id() + " event " + event
			+ " at " + definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static void assertProjection(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestStatus status, int var0, int var1) {
		Map<String, Integer> variables = definition.definition().progressLayout().unpack(snapshot.packedVariables());
		assertEquals(status, snapshot.status());
		assertEquals(var0, variables.get("var0"));
		assertEquals(var1, variables.get("var1"));
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
			LegacyKillFlowRepairDefinitionTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			if (e instanceof QuestCompilationException compilation) {
				throw compilation;
			}
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
