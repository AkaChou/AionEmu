package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/** Start conditions are ANDed inside one group; distinct groups are alternatives (OR). */
public record QuestStartConditionGroup(List<QuestStartCondition> conditions) {
	public QuestStartConditionGroup {
		conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
	}
}
