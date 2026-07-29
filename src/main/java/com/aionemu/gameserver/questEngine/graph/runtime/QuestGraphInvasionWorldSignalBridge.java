package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.IntPredicate;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;

/**
 * 将现有漩涡与裂隙服务的活跃状态冻结为类型化世界进入事件凭据。
 * Freezes active vortex and rift service state into typed world-entry event authority.
 */
public final class QuestGraphInvasionWorldSignalBridge {

	/** 禁止实例化静态 bridge。 / Prevents instantiation of the static bridge. */
	private QuestGraphInvasionWorldSignalBridge() {
	}

	/**
	 * 使用正式世界服务构造服务器确认的世界进入快照；服务读取失败会向调用方传播。
	 * Builds a server-confirmed world-entry snapshot from production world services; service-read failures propagate to the caller.
	 */
	public static WorldEnteredEvent worldEntered(String eventId, long occurredAt, WorldEntrySnapshot snapshot) {
		return worldEntered(eventId, occurredAt, snapshot,
			worldId -> {
				var location = GameLocationBootstrapServices.vortexService().getLocationByWorld(worldId);
				return location != null && location.isActive();
			},
			worldId -> GameLocationBootstrapServices.riftService().getRiftLocations().values().stream()
				.anyMatch(location -> location.getWorldId() == worldId && location.isOpened()));
	}

	/**
	 * 使用显式 typed readers 构造可测试快照，任一通道活跃即授予本次事件访问凭据。
	 * Builds a testable snapshot from explicit typed readers and grants this event authority when either channel is active.
	 */
	public static WorldEnteredEvent worldEntered(String eventId, long occurredAt, WorldEntrySnapshot snapshot,
			IntPredicate activeVortex, IntPredicate openRift) {
		Objects.requireNonNull(snapshot, "world entry snapshot");
		Objects.requireNonNull(activeVortex, "active vortex reader");
		Objects.requireNonNull(openRift, "open rift reader");
		WorldEnteredEvent base = new WorldEnteredEvent(eventId, snapshot.playerId(), occurredAt, snapshot.worldId(),
			snapshot.instanceId(), snapshot.x(), snapshot.y(), snapshot.z(), false);
		boolean active = activeVortex.test(base.worldId()) || openRift.test(base.worldId());
		return new WorldEnteredEvent(base.eventId(), base.playerId(), base.occurredAt(), base.worldId(), base.instanceId(),
			base.x(), base.y(), base.z(), active);
	}

	/**
	 * 保存由世界加载入口提供的不可变玩家位置，不持有 Player 或 World 对象。
	 * Holds immutable player location supplied by the world-load entry point without retaining Player or World objects.
	 */
	public record WorldEntrySnapshot(int playerId, int worldId, int instanceId, float x, float y, float z) {
	}
}
