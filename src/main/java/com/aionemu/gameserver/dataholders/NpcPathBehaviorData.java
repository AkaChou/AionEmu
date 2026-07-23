package com.aionemu.gameserver.dataholders;

import java.util.Map;

public final class NpcPathBehaviorData {

	private final Map<Integer, Behavior> behaviors;

	public NpcPathBehaviorData(Map<Integer, Behavior> behaviors) {
		this.behaviors = Map.copyOf(behaviors);
	}

	public Behavior get(int npcId) {
		return behaviors.get(npcId);
	}

	public boolean allowsPathfinding(int npcId) {
		Behavior behavior = behaviors.get(npcId);
		return behavior == null || behavior.generatePathfind();
	}

	public int size() {
		return behaviors.size();
	}

	public enum PathfindFailReaction {
		RETURN_TO_SP,
		PULL_TARGET,
		ABANDON_TARGET
	}

	public record Behavior(boolean generatePathfind, String maxChaseTime, PathfindFailReaction pathfindFailReaction,
			String returnMoveType, int returnSpeedPercent, int returnSensoryPercent) {
	}
}
