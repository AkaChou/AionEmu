package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnVariant;
import com.aionemu.gameserver.services.QuestService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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

	@FunctionalInterface
	public interface VariantSelector {
		int select(int bound);
	}

	private final QuestPlayerPort players;
	private final QuestSpawnRegistry registry;
	private final SpawnCall spawn;
	private final VariantSelector variantSelector;

	public PlayerQuestSpawnPort(QuestPlayerPort players) {
		this(players, QuestSpawnRegistry.global());
	}

	public PlayerQuestSpawnPort(QuestPlayerPort players, QuestSpawnRegistry registry) {
		this(players, registry, (worldId, instanceId, templateId, x, y, z, heading) ->
				(Npc) QuestService.spawnQuestNpc(worldId, instanceId, templateId, x, y, z, heading));
	}

	public PlayerQuestSpawnPort(QuestPlayerPort players, QuestSpawnRegistry registry, SpawnCall spawn) {
		this(players, registry, spawn, bound -> ThreadLocalRandom.current().nextInt(bound));
	}

	PlayerQuestSpawnPort(QuestPlayerPort players, QuestSpawnRegistry registry, SpawnCall spawn,
		VariantSelector variantSelector) {
		this.players = Objects.requireNonNull(players, "players");
		this.registry = Objects.requireNonNull(registry, "registry");
		this.spawn = Objects.requireNonNull(spawn, "spawn");
		this.variantSelector = Objects.requireNonNull(variantSelector, "variantSelector");
	}

	@Override
	public boolean spawnNpcRandom(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
		List<QuestSpawnVariant> variants, boolean replaceExisting) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		if (variants == null || variants.isEmpty()) {
			throw new IllegalArgumentException("variants must not be empty");
		}
		if (!replaceExisting && registry.contains(snapshot, slot)) {
			return true;
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		int selected = variantSelector.select(variants.size());
		if (selected < 0 || selected >= variants.size()) {
			throw new IllegalArgumentException("variant selector returned an invalid index: " + selected);
		}
		QuestSpawnVariant variant = Objects.requireNonNull(variants.get(selected), "variant");
		ResolvedLocation resolved = resolve(snapshot, variant.location());
		if (resolved == null) {
			return false;
		}
		Npc npc = spawn.spawn(resolved.worldId, resolved.instanceId, variant.templateId(),
			resolved.x, resolved.y, resolved.z, resolved.heading);
		if (npc == null) {
			return false;
		}
		if (replaceExisting) {
			Npc previous = registry.replace(snapshot, slot, npc);
			if (previous != null && previous != npc && previous.isSpawned()) {
				previous.getController().onDelete();
			}
			return true;
		}
		if (!registry.register(snapshot, slot, npc)) {
			deleteUnregistered(npc);
			// Another after-commit execution won the slot while this NPC was being
			// created. The desired slot state is still satisfied only if that handle
			// remains authoritative; do not report an unconditional success.
			return registry.contains(snapshot, slot);
		}
		return true;
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
		if (!registry.register(snapshot, slot, npc)) {
			deleteUnregistered(npc);
			return registry.contains(snapshot, slot);
		}
		return true;
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

	private static void deleteUnregistered(Npc npc) {
		if (npc.isSpawned() && npc.getController() != null) {
			npc.getController().onDelete();
		}
	}

	private record ResolvedLocation(int worldId, int instanceId, float x, float y, float z, byte heading) {
	}
}
