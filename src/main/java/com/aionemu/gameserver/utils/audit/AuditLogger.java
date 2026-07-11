package com.aionemu.gameserver.utils.audit;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.google.common.base.Preconditions;

/**
 * 审计日志工具：记录玩家异常行为，可选触发自动处罚并广播给 GM。
 * Audit logger for player misbehavior; optionally auto-punishes and broadcasts to GMs.
 *
 * @author MrPoke
 */
@Slf4j(topic = "AUDIT_LOG")
public class AuditLogger {

	/**
	 * 记录玩家审计信息；开启处罚时触发 {@link AutoBan}。
	 * Logs player audit info; triggers {@link AutoBan} when punishment is enabled.
	 *
	 * @param player 玩家（不可为 null） / player (must not be null)
	 * audit message
	 */
	public static final void info(Player player, String message) {
		Preconditions.checkNotNull(player, "Player should not be null or use different info method");
		if (LoggingConfig.LOG_AUDIT) {
			info(player.getName(), player.getObjectId(), message);
		}
		if (PunishmentConfig.PUNISHMENT_ENABLE) {
			AutoBan.punishment(player, message);
		}
	}

	/**
	 * 按角色名与 objectId 记录审计日志，并可向在线 GM 广播。
	 * Logs audit by character name and objectId, optionally broadcasting to online GMs.
	 *
	 * character name
	 * object id
	 * audit message
	 */
	public static final void info(String playerName, int objectId, String message) {
		message += " Player name: " + playerName + " objectId: " + objectId;
		log.info(message);

		if (SecurityConfig.GM_AUDIT_MESSAGE_BROADCAST) {
			GameRuntimeServices.gmService().broadcastMesage(message);
		}
	}
}
