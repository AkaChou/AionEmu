package com.aionemu.gameserver.services.player;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
/**
 * 玩家聊天服务，处理刷屏检测与聊天日志。
 * Player chat service handling flood detection and chat logging.
 */
@Slf4j(topic = "CHAT_LOG")

public class PlayerChatService {

	/**
	 * isFlooding 方法。
	 * isFlooding method.
	 *
	 * 玩家 / player
	 * result
	 */
	public static boolean isFlooding(final Player player) {
		player.setLastMessageTime();
		if (player.floodMsgCount() > SecurityConfig.FLOOD_MSG) {
			player.setGagged(true);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FLOODING);
			player.getController().cancelTask(TaskId.GAG);
			player.getController().addTask(TaskId.GAG, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					player.setGagged(false);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CAN_CHAT_NOW);
				}
			}, 2 * 60000L));
			return true;
		}
		return false;
	}

	/**
	 * 记录聊天日志。
	 * Logs chat messages.
	 *
	 * 玩家 / player
	 * type
	 * message
	 */
	public static void chatLogging(Player player, ChatType type, String message) {
		switch (type) {
		case GROUP:
			log.info(I18n.get("log.066f93159908", player.getCurrentTeamId(), player.getName(), message));
			break;
		case ALLIANCE:
			log.info(I18n.get("log.8eb6d0b29e0d", player.getCurrentTeamId(), player.getName(), message));
			break;
		case GROUP_LEADER:
			log.info(I18n.get("log.d5cda3f6bbc4", player.getName(), message));
			break;
		case LEGION:
			log.info(I18n.get("log.6d6ea2f34162", player.getLegion().getLegionName(), player.getName(), message));
			break;
		case LEAGUE:
		case LEAGUE_ALERT:
			log.info(I18n.get("log.9846ce222a32", player.getCurrentTeamId(), player.getName(), message));
			break;
		case NORMAL:
		case SHOUT:
			if (player.getRace() == Race.ASMODIANS) {
				log.info(I18n.get("log.03d19260828c", player.getName(), message));
			} else {
				log.info(I18n.get("log.bc1bf4c81b27", player.getName(), message));
			}
			break;
		default:
			if (player.isGM()) {
			log.info(I18n.get("log.94b09c84bcf4", player.getName(), message));
			}
			break;
		}
	}
}
