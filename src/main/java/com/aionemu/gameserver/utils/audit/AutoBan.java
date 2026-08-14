package com.aionemu.gameserver.utils.audit;

import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.PunishmentService;

/**
 * 自动处罚：按 {@link PunishmentConfig} 类型对审计命中玩家执行踢线/封角色/封账号/封 IP/封 MAC。
 * Auto-ban that applies kick/char/account/IP/MAC punishments by {@link PunishmentConfig} type.
 *
 * @author synchro2
 */
public class AutoBan {

	/**
	 * 对玩家执行配置的自动处罚。
	 * Applies the configured automatic punishment to the player.
	 *
	 * @param player 目标玩家 / target player
	 * @param message 处罚原因消息 / punishment reason message
	 */
	protected static void punishment(Player player, String message) {

		String reason = "AUTO " + message;
		String address = player.getClientConnection().getMacAddress();
		String accountIp = player.getClientConnection().getIP();
		int accountId = player.getClientConnection().getAccount().getId();
		int playerId = player.getObjectId();
		int time = PunishmentConfig.PUNISHMENT_TIME;
		int minInDay = 1440;
		int dayCount = (int) (Math.floor((double) (time / minInDay)));

		switch (PunishmentConfig.PUNISHMENT_TYPE) {
		case 1:
			player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
			break;
		case 2:
			PunishmentService.banChar(playerId, dayCount, reason);
			break;
		case 3:
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket((byte) 1, accountId, accountIp, time, 0);
			break;
		case 4:
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket((byte) 2, accountId, accountIp, time, 0);
			break;
		case 5:
			player.getClientConnection().closeNow();
			GameServerNetworkServices.bannedMacManager().banAddress(address, System.currentTimeMillis() + time * 60000, reason);
			break;
		}
	}
}
