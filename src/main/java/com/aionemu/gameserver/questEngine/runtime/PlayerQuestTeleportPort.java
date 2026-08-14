package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Objects;

/**
 * Real {@link QuestTeleportPort}: after commit, teleports the player via the
 * 6-arg {@link TeleportService2#teleportTo(Player, int, float, float, float, byte)}
 * which reuses the player's current instance when the target world equals the
 * current world and otherwise uses the default instance. The world id and
 * coordinates come from the compiled definition; no target/NPC guess is made.
 */
public final class PlayerQuestTeleportPort implements QuestTeleportPort {
	/** 固定签名的 teleport 委托（生产 = TeleportService2，测试 = 记录器）。 / Fixed-signature teleport delegate (production = TeleportService2, tests = recorder). */
	@FunctionalInterface
	public interface TeleportCall {
		boolean teleport(Player player, int worldId, int instanceId, float x, float y, float z, byte heading);
	}

	@FunctionalInterface
	public interface LegacyTeleportCall {
		boolean teleport(Player player, int worldId, float x, float y, float z, byte heading);
	}

	private final QuestPlayerPort players;
	private final TeleportCall teleport;

	public PlayerQuestTeleportPort(QuestPlayerPort players) {
		this(players, (TeleportCall) TeleportService2::teleportTo);
	}

	public PlayerQuestTeleportPort(QuestPlayerPort players, LegacyTeleportCall teleport) {
		this(players, (player, worldId, instanceId, x, y, z, heading) ->
			teleport.teleport(player, worldId, x, y, z, heading));
	}

	public PlayerQuestTeleportPort(QuestPlayerPort players, TeleportCall teleport) {
		this.players = Objects.requireNonNull(players, "players");
		this.teleport = Objects.requireNonNull(teleport, "teleport");
	}

	@Override
	public boolean teleportPlayer(QuestSnapshot snapshot, QuestMutationPlan plan, int worldId,
			float x, float y, float z, byte heading) {
		return teleportPlayer(snapshot, plan, QuestInstanceTarget.currentOrDefault(), worldId, x, y, z, heading);
	}

	@Override
	public boolean teleportPlayer(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestInstanceTarget instanceTarget, int worldId, float x, float y, float z, byte heading) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(instanceTarget, "instanceTarget");
		if (worldId <= 0) {
			throw new IllegalArgumentException("worldId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可传送对象，best-effort 跳过。 / Commit succeeded but player logged out: no one to teleport, best-effort skip.
			return false;
		}
		int instanceId;
		if (instanceTarget instanceof QuestInstanceTarget.Fixed fixed) {
			instanceId = fixed.instanceId();
		} else if (instanceTarget instanceof QuestInstanceTarget.NextAvailable next) {
			// 优先复用玩家已注册的实例；否则分配下一个可用实例并把玩家注册进去。
			// Reuse the player's registered instance first; otherwise allocate the next available one and register the player.
			WorldMapInstance registered = InstanceService.getRegisteredInstance(next.worldId(), player.getObjectId());
			WorldMapInstance instance = registered != null
				? registered
				: InstanceService.getNextAvailableInstance(next.worldId());
			if (registered == null) {
				InstanceService.registerPlayerWithInstance(instance, player);
			}
			instanceId = instance.getInstanceId();
		} else if (snapshot.worldId() > 0 && snapshot.instanceId() > 0) {
			instanceId = snapshot.worldId() == worldId ? snapshot.instanceId() : 1;
		} else {
			return false;
		}
		return teleport.teleport(player, worldId, instanceId, x, y, z, heading);
	}
}
