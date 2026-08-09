package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.util.Objects;
import java.util.function.IntFunction;

/** Startup gate for typed ACTION_ITEM_USE routes and quest interaction-object drops. */
public final class QuestInteractionObjectValidator {
	private QuestInteractionObjectValidator() {
	}

	public static void validate(QuestProductionDispatcher dispatcher, IntFunction<String> aiNameByTemplate) {
		Objects.requireNonNull(dispatcher, "dispatcher");
		Objects.requireNonNull(aiNameByTemplate, "aiNameByTemplate");
		for (CompiledQuestDefinition definition : dispatcher.catalogRegistry().executables()) {
			for (QuestTransition transition : definition.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.CanAct canAct)
						|| !"ACTION_ITEM_USE".equals(canAct.actionType())) {
					continue;
				}
				String aiName = aiNameByTemplate.apply(canAct.templateId());
				if (aiName == null || aiName.isBlank()) {
					throw new IllegalStateException("quest " + definition.id() + " ACTION_ITEM_USE template "
						+ canAct.templateId() + " has no NPC template/AI");
				}
				if (!"quest_use_item".equals(aiName)) {
					continue;
				}
				boolean explicitTalk = definition.definition().transitions().stream()
					.map(QuestTransition::event)
					.filter(QuestEvent.TalkToNpc.class::isInstance)
					.map(QuestEvent.TalkToNpc.class::cast)
					.anyMatch(talk -> talk.npcId() == canAct.templateId());
				boolean hasDrop = dispatcher.questDrops(canAct.templateId()).stream()
					.anyMatch(drop -> drop.questId() == definition.id());
				if (!explicitTalk && !hasDrop) {
					throw new IllegalStateException("quest " + definition.id() + " quest_use_item template "
						+ canAct.templateId() + " has neither TALK route nor catalog drop metadata");
				}
				if (!explicitTalk && (!Objects.equals(transition.sourceNode(), transition.targetNode())
						|| !transition.actions().isEmpty() || !transition.afterCommit().isEmpty())) {
					throw new IllegalStateException("quest " + definition.id() + " quest_use_item template "
						+ canAct.templateId() + " requires an explicit TALK route for side effects");
				}
			}
		}
	}
}
