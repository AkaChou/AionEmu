package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 证明自动 Journey 只从生产 catalog/XML 和客户端资源规划，
 * 并能完整执行已由 Aion 5.8 客户端验收的 1913、14047。
 * Proves automatic journeys are planned only from the production catalog/XML and client resources and can execute
 * the Aion 5.8 client-accepted quests 1913 and 14047 to completion.
 */
class QuestProductionJourneyTest {
	private static QuestCatalog catalog;
	private static ClientResourceOracle oracle;

	@BeforeAll
	static void loadProductionSources() throws Exception {
		catalog = QuestDefinitionDirectoryLoader.compile(QuestProductionJourneyTest.class.getClassLoader());
		oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
	}

	@Test
	void plansAndExecutesQuest1913FromProductionXml() throws Exception {
		assertProductionJourneyCompletes(1913);
	}

	@Test
	void plansAndExecutesQuest14047FromProductionXml() throws Exception {
		assertProductionJourneyCompletes(14047);
	}

	@Test
	void plansAndExecutesQuest2223ThroughItsRewardOwnerFromProductionXml() throws Exception {
		assertProductionJourneyCompletes(2223);
	}

	/**
	 * 1220 宝箱交接链：乌内接取发箱(182200568)→努蒙换箱(182200569)→玛平恩恩结算，
	 * 覆盖此前“接取不发箱导致 SETPRO1 物品不足回退动作 ID 触发 load fail”的缺陷。
	 * Quest 1220 box hand-off chain: Une starts and grants 182200568, Numonerk exchanges it for 182200569,
	 * Mappinerk settles the reward, covering the missing-treasure-box defect that echoed an action id
	 * as a page and produced a client "load fail".
	 */
	@Test
	void plansAndExecutesSecretDeliveryBoxChainFromProductionXml() throws Exception {
		CompiledQuestDefinition definition = definition(1220);
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	@Test
	void executesTargetlessItemStartAndNormalizedTransactions() throws Exception {
		for (int questId : List.of(1106, 1114, 1843)) assertProductionJourneyCompletes(questId);
	}

	@Test
	void plansAndExecutesTargetlessNpcFactionAcquisitionFromProductionXml() throws Exception {
		CompiledQuestDefinition definition = definition(49715);
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);

		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		assertEquals(QuestProductionJourneyPlanner.StepKind.TARGETLESS_ACTION,
			planned.plan().steps().getFirst().kind());
		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	@Test
	void executesFinishDialogAndDeterministicObjectDropsFromProductionXml() throws Exception {
		CompiledQuestDefinition definition = definition(1103);
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);

		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		assertFalse(planned.plan().initialInventory().containsKey(182200201));
		assertEquals(1, planned.plan().steps().stream()
			.filter(step -> step.kind() == QuestProductionJourneyPlanner.StepKind.PAGE_ACTION)
			.filter(step -> step.transition().event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203057
				&& talk.dialogId() == QuestDialogAction.FINISH_DIALOG.id())
			.count());
		assertEquals(0, planned.plan().steps().stream()
			.filter(step -> step.kind() == QuestProductionJourneyPlanner.StepKind.CLIENT_LOCAL_FINISH_DIALOG).count());
		assertEquals(3, planned.plan().steps().stream()
			.filter(step -> step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT_DROP).count());
		assertTrue(planned.plan().steps().stream()
			.filter(step -> step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT_DROP)
			.allMatch(step -> step.metadataDrop().npcId() == 700105 && step.metadataDrop().itemId() == 182200201
				&& step.metadataDrop().chance() == 100));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());

		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
		int finishDialogIndex = java.util.stream.IntStream.range(0, planned.plan().steps().size())
			.filter(index -> planned.plan().steps().get(index).kind()
				== QuestProductionJourneyPlanner.StepKind.PAGE_ACTION)
			.filter(index -> planned.plan().steps().get(index).transition().event()
				instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203057
				&& talk.dialogId() == QuestDialogAction.FINISH_DIALOG.id())
			.findFirst().orElseThrow();
		var finishDialog = executed.steps().get(finishDialogIndex);
		assertTrue(finishDialog.outcome().handled());
		assertFalse(finishDialog.outcome().failed());
		assertEquals(0, finishDialog.page());
		assertTrue(finishDialog.outcome().packets().stream()
			.anyMatch(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
				&& packet.dialogId() == 0));
		assertEquals(List.of(1, 2, 3), java.util.stream.IntStream.range(0, planned.plan().steps().size())
			.filter(index -> planned.plan().steps().get(index).kind()
				== QuestProductionJourneyPlanner.StepKind.USE_OBJECT_DROP)
			.map(index -> executed.steps().get(index).inventory().getOrDefault(182200201, 0)).boxed().toList());
	}

	@Test
	void movieEndIsPlannedOnlyAfterTheMatchingMovieStarts() throws Exception {
		CompiledQuestDefinition definition = definition(1170);
		QuestProductionJourneyPlanner.Plan plan = new QuestProductionJourneyPlanner()
			.plan(definition, oracle).plan();
		int movieEndIndex = java.util.stream.IntStream.range(0, plan.steps().size())
			.filter(index -> plan.steps().get(index).transition().event() instanceof QuestEvent.MovieEnd(int movieId)
				&& movieId == 16)
			.findFirst().orElseThrow();

		assertTrue(movieEndIndex > 0);
		assertTrue(plan.steps().get(movieEndIndex - 1).transition().afterCommit().stream()
			.anyMatch(action -> action instanceof AfterCommitAction.PlayMovie movie && movie.movieId() == 16));
		assertProductionJourneyCompletes(1170);
	}

	@Test
	void auditRowsReportBothAcceptedProductionJourneysAsComplete() throws Exception {
		List<CompiledQuestDefinition> definitions = List.of(definition(1913), definition(14047));
		List<QuestProductionJourneyAudit.Row> rows = QuestProductionJourneyAudit.audit(definitions, oracle);
		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(row -> "COMPLETE".equals(row.status())), rows::toString);
	}

	@Test
	void stopsAtTheFirstUnreachableClientStep() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestProductionJourneyPlanner.Plan productionPlan = new QuestProductionJourneyPlanner()
			.plan(definition, oracle).plan();
		QuestProductionJourneyPlanner.Plan brokenPlan = new QuestProductionJourneyPlanner.Plan(
			definition.id(), productionPlan.steps().get(1).transition().sourceNode(), productionPlan.completeNode(),
			productionPlan.playerClass(), productionPlan.initialInventory(),
			productionPlan.steps().subList(1, productionPlan.steps().size()));

		QuestProductionJourneyExecutor.Result result = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, brokenPlan);

		assertFalse(result.completed());
		assertEquals(0, result.failure().stepIndex());
		assertEquals(QuestE2eStatus.CLICK_NO_RESPONSE, result.failure().status());
	}

	private static void assertProductionJourneyCompletes(int questId) throws Exception {
		CompiledQuestDefinition definition = definition(questId);
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		assertNotNull(planned.plan());
		QuestProductionJourneyExecutor.Result result = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(result.completed(), () -> String.valueOf(result.failure()));
	}

	private static CompiledQuestDefinition definition(int questId) {
		return catalog.findExecutable(questId).orElseThrow();
	}
}
