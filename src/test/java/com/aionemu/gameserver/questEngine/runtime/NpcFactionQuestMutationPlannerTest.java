package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFactionQuestMutationPlannerTest {
	@Test
	void schedulesFactionStartAndCompletionForNonTimeBasedQuest() {
		CompiledQuestDefinition definition = compile(false);

		var start = definition.definition().transitions().get(0);
		var startPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 9001, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()),
			new QuestEvent.TalkToNpc(700001, 1002), start).orElseThrow();
		assertTrue(startPlan.afterCommit().stream()
			.anyMatch(AfterCommitAction.StartNpcFactionQuest.class::isInstance));

		var complete = definition.definition().transitions().stream()
			.filter(transition -> "complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		var completePlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 9001, QuestStatus.REWARD, 0, Map.of()),
			new QuestEvent.TalkToNpc(700001, 8), complete).orElseThrow();
		assertTrue(completePlan.afterCommit().stream()
			.anyMatch(AfterCommitAction.CompleteNpcFactionQuest.class::isInstance));
	}

	@Test
	void startsFactionLifecycleForDailyQuestOnAccept() {
		// 真端依据:阵营任务 daily 标志不可靠,接取即启动生命周期,不再按 timeBased 取消。
		CompiledQuestDefinition definition = compile(true);
		var start = definition.definition().transitions().get(0);

		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 9001, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()),
			new QuestEvent.TalkToNpc(700001, 1002), start).orElseThrow();

		assertTrue(plan.afterCommit().stream()
			.anyMatch(AfterCommitAction.StartNpcFactionQuest.class::isInstance));
	}

	private static CompiledQuestDefinition compile(boolean timeBased) {
		String repeat = timeBased ? "<repeat max-repeat-count=\"255\" cooldown-seconds=\"0\" daily=\"true\" weekly=\"false\" cycles=\"ALL\"/>" : "";
		String xml = """

						<quest-definition id="9001" version="1">
						  <metadata name="faction" display-name-id="0" min-level="1" max-level="55" category="FACTION" npc-faction-id="4">
						    %s
						    <rewards><reward kind="EXP" id="0" amount="1"/></rewards>
						  </metadata>
						  <nodes>
						    <node label="unaccepted" status="NONE"/>
						    <node label="started" status="START"/>
						    <node label="reward" status="REWARD"/>
						    <node label="complete" status="COMPLETE"/>
						  </nodes>
						  <transitions>
						    <transition source="unaccepted" target="started">
						      <event><talk-to-npc npc-id="700001" dialog-id="1002"/></event>
						      <conditions><start-eligible/></conditions>
						    </transition>
						    <transition source="started" target="reward">
						      <event><talk-to-npc npc-id="700001" dialog-id="1"/></event>
						    </transition>
						    <transition source="reward" target="complete">
						      <event><talk-to-npc npc-id="700001" dialog-id="8"/></event>
						      <actions><complete-quest reward-index="0"/></actions>
						      <after-commit><sync-quest-state mode="COMPLETION"/></after-commit>
						    </transition>
						  </transitions>
						</quest-definition>

				""".formatted(repeat);
		return QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}
}
