package com.aionemu.gameserver.questEngine.model;

/** 真端 NPC AI 使用的玩家任务状态。 */
public enum RetailQuestState {
	QSTATEI_NONE,
	QSTATEI_ACQUIRED,
	QSTATEI_SUCCEED;

	public boolean matches(QuestState questState) {
		QuestStatus status = questState == null ? null : questState.getStatus();
		return switch (this) {
			case QSTATEI_NONE -> status != QuestStatus.START && status != QuestStatus.REWARD
				&& status != QuestStatus.COMPLETE;
			case QSTATEI_ACQUIRED -> status == QuestStatus.START || status == QuestStatus.REWARD;
			case QSTATEI_SUCCEED -> status == QuestStatus.COMPLETE;
		};
	}
}
