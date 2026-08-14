package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;

import java.util.Objects;
import java.util.Set;

/** 在任务事件边界捕获的不可变会员权限事实。 / Immutable membership-permission facts captured at the quest event boundary. */
public record QuestMembershipFacts(Set<QuestMembershipPermission> granted) {
	public QuestMembershipFacts {
		Objects.requireNonNull(granted, "granted");
		if (granted.stream().anyMatch(permission -> permission == null)) {
			throw new IllegalArgumentException("granted membership permissions must not contain null");
		}
		granted = Set.copyOf(granted);
	}

	public boolean has(QuestMembershipPermission permission) {
		return permission != null && granted.contains(permission);
	}
}
