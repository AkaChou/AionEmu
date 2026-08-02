package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.services.QuestService;

import java.util.Objects;

/**
 * Real {@link QuestSpawnPort}: after commit, spawns/despawns quest NPCs through
 * {@link QuestService#spawnQuestNpc} and tracks the authoritative handle in the
 * {@link QuestSpawnRegistry}. instanceId 策略:目标世界等于玩家当前世界时复用玩家实例,
 * 否则用默认实例 1 (与传送端口一致,不猜测)。slot 幂等,不重复刷怪。
 */
public final class PlayerQuestSpawnPort implements QuestSpawnPort {
	/** 固定签名的 spawn 委托 (生产 = QuestService, 测试 = 记录器)。 */
	@FunctionalInterface
	public interface SpawnCall {
		Npc spawn(int worldId, int instanceId, int templateId, float x, float y, float z, byte heading);
	}

	private final QuestPlayerPort players;
	private final QuestSpawnRegistry registry;
	private final SpawnCall spawn;

	public PlayerQuestSpawnPort(QuestPlayerPort players) {
		this(players, QuestSpawnRegistry.global());
	}

	public PlayerQuestSpawnPort(QuestPlayerPort players, QuestSpawnRegistry registry) {
		this(players, registry, (worldId, instanceId, templateId, x, y, z, heading) ->
				(Npc) QuestService.spawnQuestNpc(worldId, instanceId, templateId, x, y, z, heading));
	}

	public PlayerQuestSpawnPort(QuestPlayerPort players, QuestSpawnRegistry registry, SpawnCall spawn) {
		this.players = Objects.requireNonNull(players, "players");
		this.registry = Objects.requireNonNull(registry, "registry");
		this.spawn = Objects.requireNonNull(spawn, "spawn");
	}

	@Override
	public boolean spawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int templateId,
			QuestSpawnLocation location) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(location, "location");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		if (templateId <= 0) {
			throw new IllegalArgumentException("templateId must be positive");
		}
		if (registry.contains(snapshot, slot)) {
			return true;
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出:无可参考实例,best-effort 跳过。
			return false;
		}
		ResolvedLocation resolved = resolve(snapshot, location);
		if (resolved == null) {
			return false;
		}
		Npc npc = spawn.spawn(resolved.worldId, resolved.instanceId, templateId,
			resolved.x, resolved.y, resolved.z, resolved.heading);
		if (npc == null) {
			return false;
		}
		return registry.register(snapshot, slot, npc);
	}

	@Override
	public boolean despawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		Npc npc = registry.remove(snapshot, slot);
		if (npc == null) {
			return true;
		}
		if (npc.isSpawned()) {
			npc.getController().onDelete();
		}
		return true;
	}

	private static ResolvedLocation resolve(QuestSnapshot snapshot, QuestSpawnLocation location) {
		if (location instanceof QuestSpawnLocation.PlayerPosition playerPosition) {
			if (snapshot.worldId() <= 0 || snapshot.instanceId() <= 0) {
				return null;
			}
			return new ResolvedLocation(snapshot.worldId(), snapshot.instanceId(), snapshot.x(), snapshot.y(),
				snapshot.z(), playerPosition.heading());
		}
		QuestSpawnLocation.Fixed fixed = (QuestSpawnLocation.Fixed) location;
		int instanceId;
		if (fixed.instanceTarget() instanceof QuestInstanceTarget.Fixed target) {
			instanceId = target.instanceId();
		} else if (snapshot.worldId() > 0 && snapshot.instanceId() > 0) {
			instanceId = snapshot.worldId() == fixed.worldId() ? snapshot.instanceId() : 1;
		} else {
			return null;
		}
		return new ResolvedLocation(fixed.worldId(), instanceId, fixed.x(), fixed.y(), fixed.z(), fixed.heading());
	}

	private record ResolvedLocation(int worldId, int instanceId, float x, float y, float z, byte heading) {
	}
}
