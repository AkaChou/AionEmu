package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.geo.path.PathService;

/**
 * 管理员地理/寻路诊断：高度对比与 PathService 队列指标。
 * Admin geo/path diagnostics: height check and PathService queue metrics.
 */
public class Geo extends AdminCommand {

	/**
	 * 注册 {@code //geo} 命令。
	 * Registers the {@code //geo} command.
	 */
	public Geo() {
		super("geo");
	}

	/**
	 * 执行地理/寻路诊断：输出玩家高度或 PathService 与 NPC 移动恢复的队列指标。
	 * Executes geo/path diagnostics: prints player height or PathService and NPC move-recovery queue metrics.
	 *
	 * @param params 参数：z 或 path / z or path
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			onFail(player, null);
			return;
		}
		if ("z".startsWith(params[0])) {
			com.aionemu.gameserver.model.gameobjects.VisibleObject target = player.getTarget();
			com.aionemu.gameserver.model.gameobjects.Creature subject = target instanceof com.aionemu.gameserver.model.gameobjects.Creature c ? c : player;
			int worldId = subject.getWorldId();
			float curZ = subject.getZ();
			float geoZ = GameWorldServices.geoService().getZ(worldId, subject.getX(), subject.getY(), curZ, 100, subject.getInstanceId());
			float terrainZ = GameWorldServices.geoService().getTerrainZ(worldId, subject.getX(), subject.getY());
			float[] pathGround = GameWorldServices.pathService().projectGroundPoint(subject, subject.getX(), subject.getY(), curZ);
			String spawnInfo = "";
			if (subject.getSpawn() != null) {
				spawnInfo = " spawnZ=" + subject.getSpawn().getZ();
			}
			PacketSendUtility.sendMessage(player, "[" + subject.getClass().getSimpleName() + "] curZ=" + curZ
				+ " geoZ=" + geoZ + " terrainZ=" + terrainZ
				+ " pathGround=" + (pathGround == null ? "null" : pathGround[2]) + spawnInfo);
			return;
		}
		if ("path".startsWith(params[0])) {
			PathService.Metrics metrics = GameWorldServices.pathService().metrics();
			PacketSendUtility.sendMessage(player, String.format(
					"PATH submitted=%d completed=%d rejected=%d timedOut=%d queueExpired=%d cacheHits=%d queued=%d active=%d avgQueueMicros=%d avgSearchMicros=%d",
					metrics.submitted(), metrics.completed(), metrics.rejected(), metrics.timedOut(), metrics.queueExpired(),
					metrics.cacheHits(), metrics.queued(), metrics.active(), metrics.averageQueueMicros(), metrics.averageMicros()));
			PacketSendUtility.sendMessage(player, String.format(
					"PATH found=%d noPath=%d invalidPosition=%d nodeLimit=%d interrupted=%d cancelled=%d failed=%d nodes=%d recovery=%d/%d/%d",
					metrics.found(), metrics.noPath(), metrics.invalidPosition(), metrics.nodeLimit(), metrics.interrupted(),
					metrics.cancelled(), metrics.failed(), metrics.processedNodes(), metrics.recoverySubmitted(),
					metrics.recoveryFound(), metrics.recoveryFailed()));
			PacketSendUtility.sendMessage(player, String.format(
					"PATH hierarchical=%d/%d/%d abstractNodes=%d",
					metrics.hierarchicalAttempts(), metrics.hierarchicalFound(), metrics.hierarchicalFallbacks(),
					metrics.abstractNodes()));
			PacketSendUtility.sendMessage(player, String.format(
					"PATH smooth=%d/%d geoSegments=%d/%d",
					metrics.pathsBeforeSmooth(), metrics.pathsAfterSmooth(), metrics.geoSegmentChecks(),
					metrics.geoSegmentRejected()));
			NpcMoveController.RecoveryMetrics recovery = NpcMoveController.recoveryMetrics();
			PacketSendUtility.sendMessage(player, String.format(
					"PATH recovery suspected=%d confirmed=%d selfRecovered=%d replan=%d/%d/%d skip=%d/%d nearest=%d/%d",
					recovery.stuckSuspected(), recovery.stuckConfirmed(), recovery.stuckSelfRecovered(),
					recovery.replanAttempts(), recovery.replanFound(), recovery.replanFailed(),
					recovery.waypointSkipAttempts(), recovery.waypointSkipSuccess(), recovery.nearestNodeAttempts(),
					recovery.nearestNodeSuccess()));
			return;
		}
		onFail(player, null);
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Usage: //geo z | //geo path");
	}
}
