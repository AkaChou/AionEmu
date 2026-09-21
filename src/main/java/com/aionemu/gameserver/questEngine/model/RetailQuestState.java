package com.aionemu.gameserver.questEngine.model;

/** 真实 NPC AI 使用的玩家任务状态。
 * Retail quest states used by retail-style NPC AI. */
public enum RetailQuestState {
	QSTATEI_NONE,
	QSTATEI_ACQUIRED,
	QSTATEI_SUCCEED;

	public boolean matches(QuestState questState) {
		QuestStatus status = questState == null ? null : questState.getStatus();
        switch (this) {
            case QSTATEI_NONE:
                return status != QuestStatus.START && status != QuestStatus.REWARD
                        && status != QuestStatus.COMPLETE;
            case QSTATEI_ACQUIRED:
                return status == QuestStatus.START || status == QuestStatus.REWARD;
            case QSTATEI_SUCCEED:
                return status == QuestStatus.COMPLETE;
            default:
                throw new IllegalArgumentException();
        }
	}
}
