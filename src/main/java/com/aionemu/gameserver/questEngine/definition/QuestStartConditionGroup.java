package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/**
 * 组内起始条件为 AND 关系；不同组之间为 OR 备选关系。
 * Start conditions are ANDed inside one group; distinct groups are alternatives (OR).
 */
public record QuestStartConditionGroup(List<QuestStartCondition> conditions) {
	public QuestStartConditionGroup {
		conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
	}
}
