package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Unique shadow gate key for one candidate event/path/dispatch contract. */
public record QuestShadowCoverageKey(int questId, String eventType, String eventSelector,
		String sourceNode, String targetNode, Integer priority, String dispatchContract) {
	private static final String UNWIRED = "UNWIRED";

	public QuestShadowCoverageKey {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		eventType = requireText(eventType, "eventType");
		eventSelector = requireText(eventSelector, "eventSelector");
		sourceNode = requireText(sourceNode, "sourceNode");
		targetNode = requireText(targetNode, "targetNode");
		if (priority != null && priority < 0) {
			throw new IllegalArgumentException("priority must be non-negative");
		}
		dispatchContract = requireText(dispatchContract, "dispatchContract");
	}

	static QuestShadowCoverageKey expected(CompiledQuestDefinition definition, QuestTransition transition) {
		return key(definition, transition, transition.event().type(), expectedContract(transition.event()));
	}

	static QuestShadowCoverageKey observed(CompiledQuestDefinition definition, QuestTransition transition,
			String actualEventType, QuestDispatchContract actualContract) {
		return key(definition, transition, actualEventType, Objects.requireNonNull(actualContract, "actualContract").name());
	}

	private static QuestShadowCoverageKey key(CompiledQuestDefinition definition, QuestTransition transition,
			String eventType, String contract) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(transition, "transition");
		return new QuestShadowCoverageKey(definition.id(), eventType, transition.event().toString(),
			resolveSource(definition, transition), transition.targetNode(), transition.priority(), contract);
	}

	private static String resolveSource(CompiledQuestDefinition definition, QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return transition.sourceNode();
		}
		Set<QuestStatus> statuses = transition.conditions().stream()
			.filter(QuestCondition.StatusIs.class::isInstance)
			.map(QuestCondition.StatusIs.class::cast)
			.map(QuestCondition.StatusIs::status)
			.collect(java.util.stream.Collectors.toSet());
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			List<QuestNode> matches = definition.definition().nodes().stream()
				.filter(node -> node.projection().status() == status).toList();
			if (matches.size() == 1) {
				return matches.get(0).label();
			}
		}
		if (definition.definition().nodes().size() == 1) {
			return definition.definition().nodes().get(0).label();
		}
		throw new IllegalStateException("compiled transition has no stable source node: " + definition.id());
	}

	private static String expectedContract(QuestEvent event) {
		return switch (event.type()) {
			case "TALK_TO_NPC", "ZONE_MISSION_END", "MOVIE_END", "FAIL_CRAFT", "CAN_ACT",
				"NPC_REACH_TARGET", "NPC_LOST_TARGET" -> QuestDispatchContract.EXCLUSIVE.name();
			case "USE_ITEM" -> QuestDispatchContract.FIRST_NON_UNKNOWN.name();
			case "BONUS_APPLY" -> QuestDispatchContract.FIRST_REGISTERED.name();
			case "KILL_NPC", "ATTACK_NPC", "HOUSE_ITEM_USE", "GET_ITEM", "LEVEL_UP", "DIE",
				"LOG_OUT", "ABANDON", "ENTER_WORLD", "ENTER_ZONE", "LEAVE_ZONE", "PASS_FLYING_RING",
				"QUEST_TIMER_END", "INVISIBLE_TIMER_END", "KILL_RANKED", "KILL_IN_WORLD", "USE_SKILL",
				"EQUIP_ITEM", "DREDGION_REWARD", "KAMAR_REWARD", "OPHIDAN_REWARD", "BASTION_REWARD",
				"ADD_AGGRO_LIST", "AT_DISTANCE", "ENTER_WIND_STREAM", "RIDE_ACTION", "CREATIVITY_POINT"
				-> QuestDispatchContract.BROADCAST.name();
			case "COLLECT_ITEM", "ITEM_PLAY", "PROTECT_END", "PROTECT_FAIL" -> UNWIRED;
			default -> throw new IllegalArgumentException("unknown quest event type: " + event.type());
		};
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
