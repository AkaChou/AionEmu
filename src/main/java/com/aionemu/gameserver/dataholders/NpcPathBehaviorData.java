package com.aionemu.gameserver.dataholders;

import java.util.Map;

/**
 * NPC 寻路失败行为数据容器：按 NPC ID 索引寻路失败时的处理行为。
 * NPC path-find failure behavior holder: indexes fail behaviors by NPC id.
 */
public final class NpcPathBehaviorData {

	private final Map<Integer, Behavior> behaviors;

	public NpcPathBehaviorData(Map<Integer, Behavior> behaviors) {
		this.behaviors = Map.copyOf(behaviors);
	}

	/**
	 * 返回按 NPC ID 索引的寻路行为只读视图，供合并加载复用扫描结果。
	 * Read-only view of path behaviors by NPC id, letting merged loads reuse the scan result.
	 */
	public Map<Integer, Behavior> behaviors() {
		return behaviors;
	}

	public Behavior get(int npcId) {
		return behaviors.get(npcId);
	}

	public int size() {
		return behaviors.size();
	}

	/**
	 * 寻路失败时的应对反应类型。
	 * Reaction types when path-finding fails.
	 */
	public enum PathfindFailReaction {
		/** 返回出生点。 / Returns to the spawn point. */
		RETURN_TO_SP,
		/** 拉向目标。 / Pulls toward the target. */
		PULL_TARGET,
		/** 放弃目标。 / Abandons the target. */
		ABANDON_TARGET
	}

	/**
	 * 寻路失败行为配置。
	 * Path-find failure behavior configuration.
	 */
	public record Behavior(String maxChaseTime, PathfindFailReaction pathfindFailReaction,
			String returnMoveType, int returnSpeedPercent, int returnSensoryPercent) {
	}
}
