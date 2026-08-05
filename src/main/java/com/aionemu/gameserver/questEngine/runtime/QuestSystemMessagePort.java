package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestSystemMessage;
import com.aionemu.gameserver.questEngine.definition.QuestSystemMessagePacket;

/** Typed boundary for modeled system messages emitted after a quest commit. */
public interface QuestSystemMessagePort {
	boolean send(QuestSnapshot snapshot, QuestMutationPlan plan, QuestSystemMessage message);

	/** 发送任务专用客户端消息包。Sends an explicitly modeled packet for a quest-specific client message. */
	default boolean send(QuestSnapshot snapshot, QuestMutationPlan plan, QuestSystemMessagePacket message) {
		throw new UnsupportedOperationException("raw quest system messages are not configured");
	}
}
