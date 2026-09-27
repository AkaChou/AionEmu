package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRepeatLifecycleTest {
	@Test
	void repeatable15476CanStartAgainFromCompletedState() throws Exception {
		CompiledQuestDefinition definition = definition(15476);
		QuestEvent event = new QuestEvent.TalkToNpc(805809, 1002);
		var transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("unaccepted"))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
		QuestSnapshot completed = new QuestSnapshot(7, 15476, QuestStatus.COMPLETE, 0,
			Map.of(), Map.of()).withStartEligibility(QuestStartEligibility.allowed())
			.withCompletedQuestIds(Set.of(15402));

		assertTrue(QuestMutationPlanner.plan(definition, completed, event, transition).isPresent());
	}

	@Test
	void repeatable1963ReopensItsStartPageThenAcceptsFromCompletedState() throws Exception {
		CompiledQuestDefinition definition = definition(1963);
		QuestSnapshot completed = new QuestSnapshot(7, 1963, QuestStatus.COMPLETE, 0,
			Map.of()).withStartEligibility(QuestStartEligibility.allowed());

		// S2 规范形：接取窗由 31 直发（页 4），中转（1007）退场；重复开局经定义编译期的「重复别名」
		// 把 NONE→NONE 的对话边复制到 complete 节点并附 StartEligible —— 31 在 complete 态只开窗、不改状态。
		// S2 canonical: QUEST_SELECT emits the ask window directly and the ask hop retires; the repeat
		// alias copies the NONE-to-NONE dialog edges onto the complete node with StartEligible, so 31
		// only opens the window there.
		QuestEvent reopenEvent = new QuestEvent.TalkToNpc(203726, 31);
		QuestTransition reopen = route(definition, "complete", reopenEvent);
		assertEquals(List.of(new QuestCondition.StartEligible()), reopen.conditions());
		var reopenPlan = QuestMutationPlanner.plan(definition, completed, reopenEvent, reopen).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, reopenPlan.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), reopen.afterCommit());

		// 1007 中转随规范接取段退场：complete 与 unaccepted 两侧都不再有该路由，重复开局不再依赖它。
		assertTrue(routes(definition, "complete", new QuestEvent.TalkToNpc(203726, 1007)).isEmpty(),
			"retired ask hop must not be aliased onto the complete node");
		assertTrue(routes(definition, "unaccepted", new QuestEvent.TalkToNpc(203726, 1007)).isEmpty(),
			"canonical accept flow retired the ask hop");

		QuestEvent acceptEvent = new QuestEvent.TalkToNpc(203726, 1002);
		QuestTransition accept = route(definition, "unaccepted", acceptEvent);
		var acceptPlan = QuestMutationPlanner.plan(definition, completed, acceptEvent, accept).orElseThrow();
		assertEquals(QuestStatus.START, acceptPlan.nextStatus());
		assertEquals(0, acceptPlan.nextPackedVariables());

		QuestSnapshot rejected = completed.withStartEligibility(QuestStartEligibility.rejected("REPEAT_LIMIT"));
		assertFalse(QuestMutationPlanner.plan(definition, rejected, reopenEvent, reopen).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, rejected, acceptEvent, accept).isPresent());
	}

	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, QuestEvent event) {
		return routes(definition, source, event).stream().findFirst().orElseThrow();
	}

	private static List<QuestTransition> routes(CompiledQuestDefinition definition, String source,
			QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event().equals(event))
			.toList();
	}
}
