package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.Objects;
import java.util.function.IntFunction;

/** 类型化 ACTION_ITEM_USE 路由与任务交互对象掉落的启动门禁。 / Startup gate for typed ACTION_ITEM_USE routes and quest interaction-object drops. */
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
			validateCatalogDrops(definition, aiNameByTemplate);
		}
	}

	private static void validateCatalogDrops(CompiledQuestDefinition definition,
			IntFunction<String> aiNameByTemplate) {
		for (QuestDrop drop : definition.definition().metadata().drops()) {
			if (drop.chance() <= 0 || !"quest_use_item".equals(aiNameByTemplate.apply(drop.npcId()))) {
				continue;
			}
			boolean eligible = definition.definition().transitions().stream().anyMatch(transition -> {
				if (!(transition.event() instanceof QuestEvent.CanAct canAct)
						|| canAct.templateId() != drop.npcId()
						|| !"ACTION_ITEM_USE".equals(canAct.actionType())) {
					return false;
				}
				QuestNode source = definition.definition().nodes().stream()
					.filter(node -> node.label().equals(transition.sourceNode()))
					.findFirst().orElse(null);
				return source != null
					&& source.projection().status() == QuestStatus.START
					&& (drop.collectingStep() == 0
						|| Objects.equals(source.projection().variables().get("var0"), drop.collectingStep()));
			});
			if (!eligible) {
				throw new IllegalStateException("quest " + definition.id() + " quest_use_item catalog drop npc "
					+ drop.npcId() + " item " + drop.itemId() + " collecting step " + drop.collectingStep()
					+ " has no matching START ACTION_ITEM_USE eligibility route");
			}
		}
	}
}
