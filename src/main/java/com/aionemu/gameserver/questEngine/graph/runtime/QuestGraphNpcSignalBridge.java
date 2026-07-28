package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortLostTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcProximityEvent;

/**
 * 将服务端玩家/NPC 位置快照转换为不可变任务图信号，并拒绝跨 world/instance 或越界 proximity。
 * Converts server player/NPC location snapshots into immutable quest-graph signals and rejects cross-world,
 * cross-instance, or out-of-range proximity input.
 */
public final class QuestGraphNpcSignalBridge {

	private static final float PROXIMITY_RADIUS = 20;

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphNpcSignalBridge() {
	}

	/** 表示事件源读取的服务端玩家位置快照。 / Represents a server player-location snapshot read by an event source. */
	public record PlayerSnapshot(int playerId, int worldId, int instanceId, float x, float y, float z) {
		/** 校验玩家身份、实例和有限坐标。 / Validates player identity, instance, and finite coordinates. */
		public PlayerSnapshot {
			validateLocation(playerId, worldId, instanceId, x, y, z, "Player");
		}
	}

	/** 表示事件源读取的服务端 NPC 身份与位置快照。 / Represents a server NPC identity and location snapshot read by an event source. */
	public record NpcSnapshot(int npcId, int npcObjectId, int worldId, int instanceId, float x, float y, float z) {
		/** 校验 NPC 身份、实例和有限坐标。 / Validates NPC identity, instance, and finite coordinates. */
		public NpcSnapshot {
			if (npcId <= 0) {
				throw new IllegalArgumentException("NPC template id must be positive");
			}
			validateLocation(npcObjectId, worldId, instanceId, x, y, z, "NPC");
		}
	}

	/**
	 * 从同 world/instance 的服务端位置快照生成严格 20 米内的 proximity 事件。
	 * Creates a strict-under-20-meter proximity event from same-world and same-instance server snapshots.
	 */
	public static NpcProximityEvent proximity(String eventId, long occurredAt, PlayerSnapshot player, NpcSnapshot npc) {
		validateSharedContext(player, npc);
		float distance = distance(player, npc);
		if (distance >= PROXIMITY_RADIUS) {
			throw new IllegalArgumentException("NPC proximity source is outside the fixed server radius");
		}
		return new NpcProximityEvent(eventId, player.playerId(), occurredAt, npc.npcId(), npc.npcObjectId(), npc.worldId(),
			npc.instanceId(), distance);
	}

	/** 从同 world/instance 的服务端快照生成 owner 定向的护送到达事件。 / Creates an owner-targeted escort-arrival event from same-context server snapshots. */
	public static EscortReachedTargetEvent escortReached(String eventId, long occurredAt, int questId, PlayerSnapshot player,
			NpcSnapshot npc) {
		validateSharedContext(player, npc);
		return new EscortReachedTargetEvent(eventId, player.playerId(), occurredAt, questId, npc.npcId(), npc.npcObjectId(),
			npc.worldId(), npc.instanceId());
	}

	/** 从同 world/instance 的服务端快照生成 owner 定向的护送丢失事件。 / Creates an owner-targeted escort-loss event from same-context server snapshots. */
	public static EscortLostTargetEvent escortLost(String eventId, long occurredAt, int questId, PlayerSnapshot player,
			NpcSnapshot npc) {
		validateSharedContext(player, npc);
		return new EscortLostTargetEvent(eventId, player.playerId(), occurredAt, questId, npc.npcId(), npc.npcObjectId(),
			npc.worldId(), npc.instanceId());
	}

	/** 校验玩家与 NPC 属于同一 world/instance。 / Validates that the player and NPC belong to the same world and instance. */
	private static void validateSharedContext(PlayerSnapshot player, NpcSnapshot npc) {
		if (player == null || npc == null) {
			throw new IllegalArgumentException("NPC signal snapshots are missing");
		}
		if (player.worldId() != npc.worldId() || player.instanceId() != npc.instanceId()) {
			throw new IllegalArgumentException("NPC signal snapshots cross world or instance boundaries");
		}
	}

	/** 计算服务端快照之间的三维欧氏距离。 / Computes three-dimensional Euclidean distance between server snapshots. */
	private static float distance(PlayerSnapshot player, NpcSnapshot npc) {
		double dx = (double) player.x() - npc.x();
		double dy = (double) player.y() - npc.y();
		double dz = (double) player.z() - npc.z();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** 校验对象身份、所在实例和坐标。 / Validates object identity, containing instance, and coordinates. */
	private static void validateLocation(int objectId, int worldId, int instanceId, float x, float y, float z, String label) {
		if (objectId <= 0 || worldId <= 0 || instanceId <= 0 || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException(label + " snapshot is invalid");
		}
	}
}
