package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestEventIndex;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 17014/27014 任意世界 PvP 击杀（worlds="0" 通配）迁移定义复核。
 * Retail evidence: true-server quest.xml dev_name "모든 지역에서 마족/천족 처치"
 * (kill the enemy race in ALL regions); legacy legion_task.xml
 * <kill_in_world worlds="0" amount="5"/>.
 */
class LegionTaskWorldWildcardDefinitionTest {

	private static final int[] QUEST_IDS = {17014, 27014};

	private CompiledQuestDefinition compile(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = LegionTaskWorldWildcardDefinitionTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	void bothLegionTaskWorldQuestsCompileFromProductionXml() {
		// 与同族已注册任务 17012 完全同构（dialog-ids 展开后条数一致）。
		int siblingTransitions = compile(17012).definition().transitions().size();
		for (int questId : QUEST_IDS) {
			CompiledQuestDefinition definition = compile(questId);
			assertEquals(questId, definition.id());
			assertEquals(siblingTransitions, definition.definition().transitions().size());
		}
	}

	@Test
	void eachQuestHasFiveAnyWorldKillSteps() {
		for (int questId : QUEST_IDS) {
			CompiledQuestDefinition definition = compile(questId);
			List<QuestEvent> kills = definition.definition().transitions().stream()
				.map(QuestTransition::event)
				.filter(event -> event instanceof QuestEvent.KillInWorld)
				.toList();
			assertEquals(5, kills.size(), "quest " + questId + " must advance over 5 kills");
			for (QuestEvent kill : kills) {
				assertEquals(0, ((QuestEvent.KillInWorld) kill).worldId(),
					"quest " + questId + " kill step must use the any-world wildcard");
			}
		}
	}

	@Test
	void metadataMatchesRetailQuestXml() {
		CompiledQuestDefinition elyos = compile(17014);
		assertEquals("CHALLENGE_TASK", elyos.definition().metadata().category());
		assertEquals(55, elyos.definition().metadata().minLevel());
		assertEquals(Set.of("ELYOS"), elyos.definition().metadata().permittedRaces());
		assertEquals(255, elyos.definition().metadata().repeatPolicy().maxRepeatCount());
		assertEquals(List.of(new QuestReward("EXP", 0, 4252148),
			new QuestReward("ITEM", 186000199, 10)), elyos.definition().metadata().rewards());

		CompiledQuestDefinition asmodian = compile(27014);
		assertEquals("CHALLENGE_TASK", asmodian.definition().metadata().category());
		assertEquals(55, asmodian.definition().metadata().minLevel());
		assertEquals(Set.of("ASMODIANS"), asmodian.definition().metadata().permittedRaces());
		assertEquals(255, asmodian.definition().metadata().repeatPolicy().maxRepeatCount());
		assertEquals(List.of(new QuestReward("EXP", 0, 4252148),
			new QuestReward("ITEM", 186000199, 10)), asmodian.definition().metadata().rewards());
	}

	@Test
	void anyWorldKillRoutesMatchConcreteRuntimeWorldsForBothQuests() {
		List<CompiledQuestDefinition> definitions = List.of(compile(17014), compile(27014));
		QuestEventIndex index = new QuestEventIndex(new ImmutableQuestCatalog(definitions));

		// 每条 kill-in-world 过渡各占一条 route（5 个击杀步骤），通配合并后两任务均应可路由。
		assertEquals(Set.of(17014, 27014), index.routesFor(new QuestEvent.KillInWorld(210010000)).stream()
			.map(QuestEventIndex.Route::questId).collect(Collectors.toSet()));
		assertEquals(Set.of(17014, 27014), index.routesFor(new QuestEvent.KillInWorld(600110000)).stream()
			.map(QuestEventIndex.Route::questId).collect(Collectors.toSet()));
	}

	@Test
	void fiveKillsWithPvpFactsAdvanceToK5AndReportNpcEndsQuest() {
		for (int questId : QUEST_IDS) {
			CompiledQuestDefinition definition = compile(questId);
			int packed = 0;
			for (int step = 1; step <= 5; step++) {
				QuestPvpKillFacts facts = pvpFacts(210010000);
				QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
					new QuestSnapshot(facts.recipientId(), questId, QuestStatus.START, packed, Map.of())
						.withPvpFacts(facts),
					new QuestEvent.KillInWorld(210010000, facts), killTransition(definition, packed)).orElseThrow();
				packed = plan.nextPackedVariables();
				assertEquals(step, packed, "quest " + questId + " kill " + step + " must advance var0");
			}
			// 5 kills land on k5 (var0=5), still START; the report NPC dialog moves to REWARD.
			QuestTransition report = definition.definition().transitions().stream()
				.filter(t -> "k5".equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null && talk.dialogId() == 1009)
				.findFirst().orElseThrow();
			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(1, questId, QuestStatus.START, 5, Map.of()), report).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
		}
	}

	private static QuestTransition killTransition(CompiledQuestDefinition definition, int packed) {
		return definition.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillInWorld)
			.filter(t -> definition.definition().nodes().stream()
				.filter(node -> node.label().equals(t.sourceNode()))
				.anyMatch(node -> QuestDsl.vars("var0", packed).equals(node.projection().variables())))
			.findFirst().orElseThrow();
	}

	private static QuestPvpKillFacts pvpFacts(int worldId) {
		return new QuestPvpKillFacts(1, 2, 3, 60, 60, 4, worldId, QuestPvpCreditSource.SOLO, Set.of());
	}
}
