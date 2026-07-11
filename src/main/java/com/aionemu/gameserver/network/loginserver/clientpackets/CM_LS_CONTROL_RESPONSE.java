package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.rates.Rates;
import com.aionemu.gameserver.world.World;

/**
 * 登录服对账号权限/会员控制请求的响应包，同步本地账号并通知相关玩家。
 * LoginServer response for account access/membership control; updates local account and notifies players.
 *
 * @author Aionchs-Wylovech
 */
public class CM_LS_CONTROL_RESPONSE extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_LS_CONTROL_RESPONSE(int opCode) {
		super(opCode);
	}

	private int type;
	private boolean result;
	private String playerName;
	private byte param;
	private String adminName;
	private int accountId;

	/**
	 * 读取控制类型、结果与相关玩家信息。
	 * Reads control type, result, and related player info.
	 */
	@Override
	public void readImpl() {
		type = readC();
		result = readC() == 1;
		adminName = readS();
		playerName = readS();
		param = (byte) readC();
		accountId = readD();
	}

	/**
	 * 更新本地账号并按类型向管理员/玩家发送提示。
	 * Updates the local account and notifies admin/player by type.
	 */
	@Override
	public void runImpl() {
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		Player admin = world.findPlayer(Util.convertName(adminName));
		Player player = world.findPlayer(Util.convertName(playerName));
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().accountUpdate(accountId, param, type);
		switch (type) {
		case 1:
			if (!result) {
				if (admin != null) {
					PacketSendUtility.sendMessage(admin,
							playerName + " has been promoted Administrator with role " + param);
				}
				if (player != null) {
					PacketSendUtility.sendMessage(player,
							"You have been promoted Administrator with role " + param + " by " + adminName);
				}
			} else {
				if (admin != null) {
					PacketSendUtility.sendMessage(admin, " Abnormal, the operation failed! ");
				}
			}
			break;
		case 2:
			if (!result) {
				if (admin != null) {
					PacketSendUtility.sendMessage(admin,
							playerName + " has been promoted membership with level " + param);
				}
				if (player != null) {
					player.setRates(Rates.getRatesFor(param));
					PacketSendUtility.sendMessage(player,
							"You have been promoted membership with level " + param + " by " + adminName);
				}
			} else {
				if (admin != null) {
					PacketSendUtility.sendMessage(admin, " Abnormal, the operation failed! ");
				}
			}
			break;
		}
	}
}
