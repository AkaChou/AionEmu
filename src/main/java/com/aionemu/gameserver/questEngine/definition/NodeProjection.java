package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 编译期节点标签投影出的规范状态与打包变量。
 * Compile-time node label projected to canonical status and packed variables.
 */
public record NodeProjection(QuestStatus status, Map<String, Integer> variables) {
	public NodeProjection {
		status = Objects.requireNonNull(status, "status");
		variables = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(variables, "variables")));
		if (variables.keySet().stream().anyMatch(name -> name == null || name.isBlank())) {
			throw new IllegalArgumentException("projection variable names must not be blank");
		}
	}
}
