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
}
