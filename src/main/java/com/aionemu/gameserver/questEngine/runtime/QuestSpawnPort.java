package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnVariant;

import java.util.List;

/** Typed boundary for the after-commit NPC spawn/despawn lifecycle. */
public interface QuestSpawnPort {
	/**
	 * Spawns a one-time quest NPC under the given slot after commit.
	 *
	 * <p>slot 是任务内编译期常量,despawn 通过它引用本事务 spawn 的权威 handle;
	 * handle 由领域注册表持有,不编码进 quest_vars。幂等:同一 slot 已有 handle 时跳过。</p>
	 *
	 * @return true 表示本次真正生成; false 表示已存在(幂等跳过)、玩家离线或失败 (best-effort)
	 */
	boolean spawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int templateId,
		QuestSpawnLocation location);

	/** Spawns one selected variant; implementations must keep slot ownership bounded. */
	default boolean spawnNpcRandom(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
		List<QuestSpawnVariant> variants, boolean replaceExisting) {
		return false;
	}

	default boolean spawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int worldId,
			int templateId, float x, float y, float z, byte heading) {
		return spawnNpc(snapshot, plan, slot, templateId, new QuestSpawnLocation.Fixed(worldId,
			QuestInstanceTarget.currentOrDefault(), x, y, z, heading));
	}

	/**
	 * Despawns the authoritative NPC spawned under this slot. 绝不凭 templateId 删任意同类。
	 *
	 * @return true 表示已删除; false 表示该 slot 无 handle (无可删) 或失败
	 */
	boolean despawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);
}
