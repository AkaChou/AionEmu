package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DredgionSettledEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RankedPlayerKillEvent;

/**
 * 将 PvP 服务和 Dredgion 实例结算的服务端快照转换为具备显式 credit/instance 权威的任务图事件。
 * Converts server snapshots from PvP service and Dredgion settlement into quest-graph events with explicit
 * credit and instance authority.
 */
public final class QuestGraphPvpSignalBridge {

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphPvpSignalBridge() {
	}

	/** 表示 PvP credit 判定读取的服务端参与者快照。 / Represents a server participant snapshot read for PvP credit evaluation. */
	public record ParticipantSnapshot(int playerId, Race race, int worldId, int instanceId, float x, float y, float z,
			boolean alive) {
		/** 校验玩家身份、玩家阵营、实例和有限坐标。 / Validates identity, player faction, instance, and finite coordinates. */
		public ParticipantSnapshot {
			if (playerId <= 0 || race != Race.ELYOS && race != Race.ASMODIANS || worldId <= 0 || instanceId <= 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("PvP participant snapshot is invalid");
			}
		}
	}

	/**
	 * 生成经过阵营、存活、同实例和 credit 距离验证的军衔玩家击杀事件。
	 * Creates a ranked-player-kill event validated for faction, survival, same-instance, and credit distance.
	 */
	public static RankedPlayerKillEvent rankedKill(String eventId, long occurredAt, ParticipantSnapshot recipient,
			ParticipantSnapshot killer, ParticipantSnapshot victim, int victimRankId, float maximumCreditDistance) {
		if (recipient == null || killer == null || victim == null || maximumCreditDistance <= 0
				|| !Float.isFinite(maximumCreditDistance)) {
			throw new IllegalArgumentException("Ranked-player-kill bridge input is invalid");
		}
		validateSharedContext(recipient, killer, victim);
		if (!recipient.alive() || recipient.race() != killer.race() || killer.race() == victim.race()
				|| killer.playerId() == victim.playerId() || recipient.playerId() == victim.playerId()) {
			throw new IllegalArgumentException("Ranked-player-kill participant authority is invalid");
		}
		float creditDistance = distance(recipient, victim);
		if (creditDistance >= maximumCreditDistance) {
			throw new IllegalArgumentException("Ranked-player-kill recipient is outside the credit radius");
		}
		return new RankedPlayerKillEvent(eventId, recipient.playerId(), occurredAt, killer.playerId(), victim.playerId(),
			victimRankId, victim.worldId(), victim.instanceId(), creditDistance, true);
	}

	/**
	 * 生成实例服务逐成员确认的 Dredgion 结算事件；死亡成员仍可按旧结算语义获得事件。
	 * Creates a Dredgion settlement event confirmed per member by the instance service; dead members remain eligible
	 * under the legacy settlement contract.
	 */
	public static DredgionSettledEvent dredgionSettled(String eventId, long occurredAt, ParticipantSnapshot member) {
		if (member == null) {
			throw new IllegalArgumentException("Dredgion settlement member snapshot is missing");
		}
		return new DredgionSettledEvent(eventId, member.playerId(), occurredAt, member.worldId(), member.instanceId());
	}

	/** 校验三名参与者属于同一 world/instance。 / Validates that all three participants belong to the same world and instance. */
	private static void validateSharedContext(ParticipantSnapshot first, ParticipantSnapshot second, ParticipantSnapshot third) {
		if (first.worldId() != second.worldId() || first.worldId() != third.worldId()
				|| first.instanceId() != second.instanceId() || first.instanceId() != third.instanceId()) {
			throw new IllegalArgumentException("PvP participants cross world or instance boundaries");
		}
	}

	/** 计算 credit recipient 与受害者的三维距离。 / Computes three-dimensional distance from credit recipient to victim. */
	private static float distance(ParticipantSnapshot recipient, ParticipantSnapshot victim) {
		double dx = (double) recipient.x() - victim.x();
		double dy = (double) recipient.y() - victim.y();
		double dz = (double) recipient.z() - victim.z();
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
