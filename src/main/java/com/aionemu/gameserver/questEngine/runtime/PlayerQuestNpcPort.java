package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.world.World;

import java.util.Objects;

/**
 * Real {@link QuestNpcPort}: after commit, deletes the interaction NPC of the
 * commit by its authoritative interactionObjectId and optionally schedules its
 * respawn. Unlike {@code DespawnNpc} (which only dereferences task-spawned
 * slots), this addresses a world static NPC (legacy
 * {@code scheduleRespawn() + onDelete()}).
 */
public final class PlayerQuestNpcPort implements QuestNpcPort {
	@FunctionalInterface
	public interface WorldProvider {
		World world();
	}

	private final WorldProvider world;

	public PlayerQuestNpcPort(WorldProvider world) {
		this.world = Objects.requireNonNull(world, "world");
	}

	@Override
	public boolean deleteInteractionNpc(QuestSnapshot snapshot, QuestMutationPlan plan, boolean scheduleRespawn) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (snapshot.interactionObjectId() <= 0) {
			// 无权威交互对象 (例如非 NPC 触发):best-effort 跳过。
			return false;
		}
		VisibleObject visible = world.world().findVisibleObject(snapshot.interactionObjectId());
		if (!(visible instanceof Npc npc)) {
			return false;
		}
		NpcController controller = npc.getController();
		if (scheduleRespawn) {
			controller.scheduleRespawn();
		}
		controller.onDelete();
		return true;
	}

	@Override
	public boolean deleteWorldNpcs(QuestSnapshot snapshot, QuestMutationPlan plan) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		var player = world.world().findPlayer(snapshot.playerId());
		if (player == null || player.getPosition() == null || player.getPosition().getWorldMapInstance() == null) {
			return false;
		}
		for (Npc npc : player.getPosition().getWorldMapInstance().getNpcs()) {
			if (npc != null && npc.isSpawned()) {
				npc.getController().onDelete();
			}
		}
		return true;
	}

	@Override
	public boolean addNpcAggro(QuestSnapshot snapshot, QuestMutationPlan plan, int npcTemplateId, int damage) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (npcTemplateId <= 0 || damage < 0) {
			throw new IllegalArgumentException("npcTemplateId must be positive and damage non-negative");
		}
		var player = world.world().findPlayer(snapshot.playerId());
		if (player == null || !player.isSpawned()) {
			return false;
		}
		boolean applied = false;
		for (VisibleObject visible : player.getKnownList().getKnownObjectsSnapshot()) {
			if (!(visible instanceof Npc npc) || npc.getNpcId() != npcTemplateId || !npc.isSpawned()
				|| npc.getWorldId() != player.getWorldId() || npc.getInstanceId() != player.getInstanceId()) {
				continue;
			}
			npc.getAggroList().addDamage(player, damage);
			applied = true;
		}
		return applied;
	}
}
