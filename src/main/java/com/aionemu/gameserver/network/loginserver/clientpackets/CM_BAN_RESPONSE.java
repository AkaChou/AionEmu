package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 登录服对封禁请求的响应包，向发起封禁的管理员回传结果消息。
 * LoginServer ban-request response that notifies the requesting admin of the outcome.
 *
 * @author Watson
 */
public class CM_BAN_RESPONSE extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_BAN_RESPONSE(int opCode) {
		super(opCode);
	}

	private byte type;
	private int accountId;
	private String ip;
	private int time;
	private int adminObjId;
	private boolean result;

	/**
	 * 读取封禁类型、目标、时长与结果。
	 * Reads ban type, target, duration, and result.
	 */
	@Override
	public void readImpl() {
		this.type = (byte) readC();
		this.accountId = readD();
		this.ip = readS();
		this.time = readD();
		this.adminObjId = readD();
		this.result = readC() == 1;
	}

	/**
	 * 按封禁类型向管理员发送成功/失败提示。
	 * Sends success/failure messages to the admin by ban type.
	 */
	@Override
	public void runImpl() {
		Player admin = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(adminObjId);

		if (admin == null) {
			return;
		}

		// 一些消息相关 / Some messages stuff
		String message;
		if (type == 1 || type == 3) {
			if (result) {
				if (time < 0) {
					message = "Account ID " + accountId + " was successfully unbanned";
				} else if (time == 0) {
					message = "Account ID " + accountId + " was successfully banned";
				} else {
					message = "Account ID " + accountId + " was successfully banned for " + time + " minutes";
				}
			} else {
				message = "Error occurred while banning player's account";
			}
			PacketSendUtility.sendMessage(admin, message);
		}
		if (type == 2 || type == 3) {
			if (result) {
				if (time < 0) {
					message = "IP mask " + ip + " was successfully removed from block list";
				} else if (time == 0) {
					message = "IP mask " + ip + " was successfully added to block list";
				} else {
					message = "IP mask " + ip + " was successfully added to block list for " + time + " minutes";
				}
			} else {
				message = "Error occurred while adding IP mask " + ip;
			}
			PacketSendUtility.sendMessage(admin, message);
		}
	}
}
