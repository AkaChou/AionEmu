package com.aionemu.gameserver.questEngine.definition;

/**
 * 附着于任务状态协议更新的精确提交后行为。
 * Exact post-commit behavior attached to a quest-state protocol update.
 */
public enum QuestStateSyncMode {
	/** 仅数据包 / packet only */
	PACKET_ONLY(false, false, false),
	/** 刷新可见性 / refresh visibility */
	VISIBILITY_REFRESH(true, false, false),
	/** 刷新等级与可见性 / refresh level and visibility */
	LEVEL_AND_VISIBILITY_REFRESH(true, true, false),
	/** 完成：刷新并通知完成 NPC / completion: refresh and notify finished NPC */
	COMPLETION(true, true, true);

	private final boolean refreshVisibility;
	private final boolean reevaluateLevelQuests;
	private final boolean notifyFinishedNpc;

	QuestStateSyncMode(boolean refreshVisibility, boolean reevaluateLevelQuests, boolean notifyFinishedNpc) {
		this.refreshVisibility = refreshVisibility;
		this.reevaluateLevelQuests = reevaluateLevelQuests;
		this.notifyFinishedNpc = notifyFinishedNpc;
	}

	public boolean refreshVisibility() {
		return refreshVisibility;
	}

	public boolean reevaluateLevelQuests() {
		return reevaluateLevelQuests;
	}

	public boolean notifyFinishedNpc() {
		return notifyFinishedNpc;
	}
}
