package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.Map;
import java.util.Objects;

/** Compile-time node label projected to canonical status and packed variables. */
public record NodeProjection(QuestStatus status, Map<String, Integer> variables) {
	public NodeProjection {
		status = Objects.requireNonNull(status, "status");
		variables = Map.copyOf(Objects.requireNonNull(variables, "variables"));
		if (variables.keySet().stream().anyMatch(name -> name == null || name.isBlank())) {
			throw new IllegalArgumentException("projection variable names must not be blank");
		}
	}
}
