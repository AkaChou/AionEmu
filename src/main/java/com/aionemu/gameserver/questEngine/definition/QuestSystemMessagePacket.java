package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/**
 * 任务自有系统消息的显式数据包形状，覆盖固定消息词汇之外的消息。
 * Explicit system-message packet shape for quest-owned messages not covered by
 * the small fixed message vocabulary.
 */
public record QuestSystemMessagePacket(int messageId, QuestSystemMessageTarget target,
		boolean npcShout, int textColorId, List<String> params) {
	public QuestSystemMessagePacket {
		if (messageId <= 0) {
			throw new IllegalArgumentException("messageId must be positive");
		}
		target = Objects.requireNonNull(target, "target");
		if (textColorId < 0 || textColorId > 255) {
			throw new IllegalArgumentException("textColorId must be between 0 and 255");
		}
		params = List.copyOf(Objects.requireNonNull(params, "params"));
		if (params.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("params must not contain null values");
		}
	}

	public QuestSystemMessagePacket(int messageId) {
		this(messageId, QuestSystemMessageTarget.NONE, false, 26, List.of());
	}
}
