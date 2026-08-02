package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;

/** Typed boundary for the after-commit teleport effect. */
public interface QuestTeleportPort {
	/**
	 * Teleports the player to the given world coordinates after commit.
	 *
	 * <p>instanceId 选择策略由实现决定 (同世界复用玩家实例, 否则默认实例 1);
	 * 不允许把 templateId 或玩家当前 target 当作目标实例。</p>
	 *
	 * @return true 表示已发起传送; false 表示失败/玩家离线 (best-effort, 记录审计)
	 */
	boolean teleportPlayer(QuestSnapshot snapshot, QuestMutationPlan plan, int worldId,
		float x, float y, float z, byte heading);

	default boolean teleportPlayer(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestInstanceTarget instanceTarget, int worldId, float x, float y, float z, byte heading) {
		if (instanceTarget != QuestInstanceTarget.CurrentOrDefault.INSTANCE) {
			throw new IllegalArgumentException("this teleport port does not support fixed instance targets");
		}
		return teleportPlayer(snapshot, plan, worldId, x, y, z, heading);
	}
}
