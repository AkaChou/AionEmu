package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestVars;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ClientQuestSectionAlignmentTest {
	private static final Set<Integer> EXTENDED_COUNTER_QUESTS = Set.of(1842, 1843, 1844, 2843, 2844, 2845);
	private static final List<Integer> VILLAGE_HUNT_QUESTS = List.of(
		17106, 17108, 17110, 17112, 17114, 17116, 17118, 17120, 17122, 17124, 17126, 17128,
		17130, 17132, 17134, 17136, 17138, 17140, 17142, 17144, 17146, 17148, 17150, 17152,
		17154);

	@Test
	void clientVisibleKillFieldsUseTheirFixedSixBitSections() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
		int checked = 0;
		for (CompiledQuestDefinition definition : catalog.executables()) {
			int questId = definition.id();
			if (EXTENDED_COUNTER_QUESTS.contains(questId)) {
				continue;
			}
			for (QuestTransition transition : definition.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.KillNpc
						|| transition.event() instanceof QuestEvent.KillNpcSet)) {
					continue;
				}
				for (QuestAction action : transition.actions()) {
					String field = touchedField(action);
					if (field == null || !field.matches("var[1-4]")) {
						continue;
					}
					BitField bitField = definition.definition().progressLayout().field(field);
					assertNotNull(bitField, () -> "quest " + questId + " lacks " + field);
					assertEquals(Integer.parseInt(field.substring(3)) * 6, bitField.offset(),
						() -> "quest " + questId + " maps " + field + " outside its client SECTION");
					checked++;
				}
			}
		}
		assertTrue(checked > 100, "production audit must inspect client-visible kill fields");
	}

	@Test
	void villageHuntsAdvanceTheLegacySectionZeroCounter() {
		for (int questId : VILLAGE_HUNT_QUESTS) {
			CompiledQuestDefinition definition = load(questId);
			QuestSnapshot state = snapshot(questId, QuestStatus.START, 0);
			for (int count = 1; count <= 3; count++) {
				state = apply(definition, state, new QuestEvent.KillNpc(219501));
				assertEquals(count, new QuestVars(state.packedVariables()).getVarById(0),
					() -> "quest " + questId + " did not update client SECTION_0");
			}
		}
	}

	@Test
	void representativeMultiSectionAndMissionCountersReachTheClientSlots() {
		CompiledQuestDefinition threeSections = load(11102);
		QuestSnapshot state = snapshot(11102, QuestStatus.START, 0);
		state = apply(threeSections, state, new QuestEvent.KillNpc(216489));
		state = apply(threeSections, state, new QuestEvent.KillNpc(216490));
		state = apply(threeSections, state, new QuestEvent.KillNpc(216491));
		QuestVars vars = new QuestVars(state.packedVariables());
		assertEquals(List.of(1, 1, 1), List.of(vars.getVarById(0), vars.getVarById(1),
			vars.getVarById(2)));

		CompiledQuestDefinition fourSections = load(18952);
		state = snapshot(18952, QuestStatus.START, 0);
		state = apply(fourSections, state, new QuestEvent.KillNpc(236225));
		state = apply(fourSections, state, new QuestEvent.KillNpc(236231));
		state = apply(fourSections, state, new QuestEvent.KillNpc(236243));
		state = apply(fourSections, state, new QuestEvent.KillNpc(236244));
		vars = new QuestVars(state.packedVariables());
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(List.of(1, 1, 1, 1), List.of(vars.getVarById(1), vars.getVarById(2),
			vars.getVarById(3), vars.getVarById(4)));

		CompiledQuestDefinition mission = load(2002);
		state = snapshot(2002, QuestStatus.START, 3);
		for (int expected = 4; expected <= 10; expected++) {
			state = apply(mission, state, new QuestEvent.KillNpc(210377));
			assertEquals(expected, new QuestVars(state.packedVariables()).getVarById(0));
		}

		CompiledQuestDefinition ceremony = load(30203);
		state = snapshot(30203, QuestStatus.START, 0);
		for (int npcId : List.of(216175, 216177, 216179, 216181, 216263)) {
			state = apply(ceremony, state, new QuestEvent.KillNpc(npcId));
		}
		vars = new QuestVars(state.packedVariables());
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(List.of(1, 1, 1, 1), List.of(vars.getVarById(0), vars.getVarById(1),
			vars.getVarById(2), vars.getVarById(3)));
	}

	@Test
	void nextKillNormalizesADeployedCompactCounter() {
		CompiledQuestDefinition definition = load(18931);
		QuestSnapshot compactFirstKill = snapshot(18931, QuestStatus.START, 1 << 2);

		QuestSnapshot normalized = apply(definition, compactFirstKill, new QuestEvent.KillNpc(243797));

		QuestVars vars = new QuestVars(normalized.packedVariables());
		assertEquals(0, vars.getVarById(0));
		assertEquals(1, vars.getVarById(1));
	}

	@Test
	void extendedEightyKillCountersRemainExplicitLegacyExceptions() {
		for (int questId : EXTENDED_COUNTER_QUESTS) {
			ProgressLayout layout = load(questId).definition().progressLayout();
			assertEquals(7, layout.field("var0").width());
			assertEquals(7, layout.field("var1").offset());
		}
	}

	private static String touchedField(QuestAction action) {
		return switch (action) {
			case QuestAction.SetVariable set -> set.field();
			case QuestAction.IncrementVariable increment -> increment.field();
			default -> null;
		};
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
				return snapshot(definition.id(), mutation.nextStatus(), mutation.nextPackedVariables());
			}
		}
		return fail("no route for quest " + definition.id() + " event " + event + " variables "
			+ definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, int packedVariables) {
		return new QuestSnapshot(7, questId, status, packedVariables, Map.of());
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				ClientQuestSectionAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
