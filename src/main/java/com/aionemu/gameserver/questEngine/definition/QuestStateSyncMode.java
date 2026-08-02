package com.aionemu.gameserver.questEngine.definition;

/** Exact post-commit behavior attached to a quest-state protocol update. */
public enum QuestStateSyncMode {
	PACKET_ONLY(false, false, false),
	VISIBILITY_REFRESH(true, false, false),
	LEVEL_AND_VISIBILITY_REFRESH(true, true, false),
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
