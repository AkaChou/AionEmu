package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcAggroListedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.PlayerSnapshot;

/**
 * 将 AI 仇恨列表和已知列表的服务端快照转换为显式 actor/recipient 的任务图感知信号。
 * Converts server aggro-list and known-list snapshots into quest-graph perception signals with explicit actor and
 * recipient authority.
 */
public final class QuestGraphAiSignalBridge {

	private static final float AGGRO_RECIPIENT_RADIUS = 50;

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphAiSignalBridge() {
	}

	/** 表示从 NPC known list 枚举得到的广播接收者快照。 / Represents a broadcast-recipient snapshot enumerated from the NPC known list. */
	public record RecipientSnapshot(PlayerSnapshot player, boolean knownToNpc) {
		/** 校验接收者确实来自服务端 known list。 / Validates that the recipient was actually read from the server known list. */
		public RecipientSnapshot {
			if (player == null || !knownToNpc) {
				throw new IllegalArgumentException("NPC aggro-list recipient authority is invalid");
			}
		}
	}

	/**
	 * 为同 world/instance 且严格位于 NPC 50 米内的接收者创建仇恨列表感知事件。
	 * Creates an aggro-list perception event for a recipient in the same world/instance and strictly within 50 meters.
	 */
	public static NpcAggroListedEvent aggroListed(String eventId, long occurredAt, RecipientSnapshot recipient,
			PlayerSnapshot aggroSource, NpcSnapshot npc) {
		if (recipient == null || aggroSource == null || npc == null) {
			throw new IllegalArgumentException("NPC aggro-list snapshots are missing");
		}
		validateSharedContext(recipient.player(), aggroSource, npc);
		float recipientDistance = distance(recipient.player(), npc);
		if (recipientDistance >= AGGRO_RECIPIENT_RADIUS) {
			throw new IllegalArgumentException("NPC aggro-list recipient is outside the fixed server radius");
		}
		return new NpcAggroListedEvent(eventId, recipient.player().playerId(), occurredAt, aggroSource.playerId(), npc.npcId(),
			npc.npcObjectId(), npc.worldId(), npc.instanceId(), recipientDistance, recipient.knownToNpc());
	}

	/** 校验 actor、recipient 和 NPC 属于同一 world/instance。 / Validates that actor, recipient, and NPC share world and instance. */
	private static void validateSharedContext(PlayerSnapshot recipient, PlayerSnapshot aggroSource, NpcSnapshot npc) {
		if (recipient.worldId() != aggroSource.worldId() || recipient.worldId() != npc.worldId()
				|| recipient.instanceId() != aggroSource.instanceId() || recipient.instanceId() != npc.instanceId()) {
			throw new IllegalArgumentException("NPC aggro-list snapshots cross world or instance boundaries");
		}
	}

	/** 计算接收者与 NPC 的三维距离。 / Computes three-dimensional distance between recipient and NPC. */
	private static float distance(PlayerSnapshot recipient, NpcSnapshot npc) {
		double dx = (double) recipient.x() - npc.x();
		double dy = (double) recipient.y() - npc.y();
		double dz = (double) recipient.z() - npc.z();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
