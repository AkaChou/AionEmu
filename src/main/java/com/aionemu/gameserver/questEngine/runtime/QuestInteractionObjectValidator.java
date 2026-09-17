package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.Objects;
import java.util.function.IntFunction;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/** 类型化 ACTION_ITEM_USE 路由与任务交互对象掉落的启动门禁。 / Startup gate for typed ACTION_ITEM_USE routes and quest interaction-object drops. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestInteractionObjectValidator {
	public static void validate(QuestProductionDispatcher dispatcher, IntFunction<String> aiNameByTemplate) {
		Objects.requireNonNull(dispatcher, "dispatcher");
		Objects.requireNonNull(aiNameByTemplate, "aiNameByTemplate");
		for (CompiledQuestDefinition definition : dispatcher.catalogRegistry().executables()) {
			validateDefinition(definition, aiNameByTemplate);
		}
	}

	/**
	 * 校验单个生产任务定义的 ACTION_ITEM_USE 合同。
	 * Validates the ACTION_ITEM_USE contract of one production quest definition.
	 */
	public static void validateDefinition(CompiledQuestDefinition definition,
			IntFunction<String> aiNameByTemplate) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(aiNameByTemplate, "aiNameByTemplate");
		for (QuestTransition transition : definition.definition().transitions()) {
			if (!(transition.event() instanceof QuestEvent.CanAct(int templateId, String actionType))
					|| !"ACTION_ITEM_USE".equals(actionType)) {
				continue;
			}
			String aiName = aiNameByTemplate.apply(templateId);
			if (aiName == null || aiName.isBlank()) {
				throw new IllegalStateException("quest " + definition.id() + " ACTION_ITEM_USE template "
					+ templateId + " has no NPC template/AI");
			}
			if (!"quest_use_item".equals(aiName)) {
				continue;
			}
			boolean explicitTalk = definition.definition().transitions().stream()
				.map(QuestTransition::event)
				.filter(QuestEvent.TalkToNpc.class::isInstance)
				.map(QuestEvent.TalkToNpc.class::cast)
				.anyMatch(talk -> talk.npcId() == templateId);
			boolean hasDrop = definition.definition().metadata().drops().stream()
				.anyMatch(drop -> drop.npcId() == templateId);
			if (!explicitTalk && !hasDrop) {
				throw new IllegalStateException("quest " + definition.id() + " quest_use_item template "
					+ templateId + " has neither TALK route nor catalog drop metadata");
			}
			if (!explicitTalk && (!Objects.equals(transition.sourceNode(), transition.targetNode())
					|| !transition.actions().isEmpty() || !transition.afterCommit().isEmpty())) {
				throw new IllegalStateException("quest " + definition.id() + " quest_use_item template "
					+ templateId + " requires an explicit TALK route for side effects");
			}
		}
		validateCatalogDrops(definition, aiNameByTemplate);
	}

	private static void validateCatalogDrops(CompiledQuestDefinition definition,
			IntFunction<String> aiNameByTemplate) {
		for (QuestDrop drop : definition.definition().metadata().drops()) {
			if (drop.chance() <= 0 || !"quest_use_item".equals(aiNameByTemplate.apply(drop.npcId()))) {
				continue;
			}
			boolean eligible = definition.definition().transitions().stream().anyMatch(transition -> {
				if (!(transition.event() instanceof QuestEvent.CanAct(int templateId, String actionType))
						|| templateId != drop.npcId()
						|| !"ACTION_ITEM_USE".equals(actionType)) {
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
