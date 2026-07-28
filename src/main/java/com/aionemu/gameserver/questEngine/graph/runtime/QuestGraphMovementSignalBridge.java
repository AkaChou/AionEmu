package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.FlyingRingPassedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WindstreamEnteredEvent;

/**
 * 将服务端 movement 校验快照转换为 windstream 与 flying-ring 任务图事件。
 * Converts server movement-validation snapshots into windstream and flying-ring quest-graph events.
 */
public final class QuestGraphMovementSignalBridge {

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphMovementSignalBridge() {
	}

	/**
	 * 表示 CM_WINDSTREAM 完成 route、位置、pending path 和飞行状态校验后的快照。
	 * Represents a CM_WINDSTREAM snapshot after route, position, pending-path, and flight-state validation.
	 */
	public record WindstreamEntrySnapshot(int playerId, int worldId, int instanceId, int teleportId, int routeId, int distance,
			boolean routePositionValidated, boolean pendingPathMatched, boolean flightStateEligible) {
		/** 校验服务端风道快照的结构边界。 / Validates structural boundaries of the server windstream snapshot. */
		public WindstreamEntrySnapshot {
			if (playerId <= 0 || worldId <= 0 || instanceId <= 0 || teleportId <= 0 || routeId <= 0 || distance < 0) {
				throw new IllegalArgumentException("Windstream-entry snapshot is invalid");
			}
		}
	}

	/**
	 * 表示 FlyRingObserver 完成平面相交和严格半径判定后的快照。
	 * Represents a FlyRingObserver snapshot after plane intersection and strict-radius validation.
	 */
	public record FlyingRingPassSnapshot(int playerId, int worldId, int instanceId, String ringName, float radius,
			float centerDistance, boolean planeIntersected, boolean intersectionPointAvailable) {
		/** 校验服务端飞行环快照的结构边界。 / Validates structural boundaries of the server flying-ring snapshot. */
		public FlyingRingPassSnapshot {
			if (playerId <= 0 || worldId <= 0 || instanceId <= 0) {
				throw new IllegalArgumentException("Flying-ring snapshot is invalid");
			}
		}
	}

	/** 创建仅由完整服务端校验授权的风道进入事件。 / Creates a windstream-entry event authorized only by complete server validation. */
	public static WindstreamEnteredEvent windstreamEntered(String eventId, long occurredAt, WindstreamEntrySnapshot snapshot) {
		if (snapshot == null) {
			throw new IllegalArgumentException("Windstream-entry snapshot is missing");
		}
		return new WindstreamEnteredEvent(eventId, snapshot.playerId(), occurredAt, snapshot.worldId(), snapshot.instanceId(),
			snapshot.teleportId(), snapshot.routeId(), snapshot.distance(), snapshot.routePositionValidated(),
			snapshot.pendingPathMatched(), snapshot.flightStateEligible());
	}

	/** 创建仅由服务端相交和严格半径检查授权的飞行环事件。 / Creates a flying-ring event authorized only by server intersection and strict-radius checks. */
	public static FlyingRingPassedEvent flyingRingPassed(String eventId, long occurredAt, FlyingRingPassSnapshot snapshot) {
		if (snapshot == null) {
			throw new IllegalArgumentException("Flying-ring snapshot is missing");
		}
		return new FlyingRingPassedEvent(eventId, snapshot.playerId(), occurredAt, snapshot.worldId(), snapshot.instanceId(),
			snapshot.ringName(), snapshot.radius(), snapshot.centerDistance(), snapshot.planeIntersected(),
			snapshot.intersectionPointAvailable());
	}
}
