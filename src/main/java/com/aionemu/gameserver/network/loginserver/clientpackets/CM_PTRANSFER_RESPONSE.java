package com.aionemu.gameserver.network.loginserver.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;

/**
 * 登录服角色转移流程响应包。
 * Login server response for player transfer flow.
 *
 * @author KID
 */
@Slf4j
public class CM_PTRANSFER_RESPONSE extends LsClientPacket {
	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_PTRANSFER_RESPONSE(int opCode) {
		super(opCode);
	}

	/**
	 * 按 actionId 读取并立即处理角色转移各阶段。
	 * Reads and immediately handles each player-transfer stage by actionId.
	 */
	@Override
	protected void readImpl() {
		int actionId = this.readD();
		switch (actionId) {
		case 20: // 发送角色信息 / send info
		{
			int targetAccount = readD();
			int taskId = readD();
			String name = readS();
			String account = readS();
			int len = readD();
			byte[] db = this.readB(len);
			GameRuntimeServices.playerTransferService().cloneCharacter(taskId, targetAccount, name, account, db);
		}
			break;
		case 21:// 成功 / ok
		{
			int taskId = readD();
			GameRuntimeServices.playerTransferService().onOk(taskId);
		}
			break;
		case 22:// 错误 / error
		{
			int taskId = readD();
			String reason = readS();
			GameRuntimeServices.playerTransferService().onError(taskId, reason);
		}
			break;
		case 23: {
			byte serverId = readSC();
			if (NetworkConfig.GAMESERVER_ID != serverId) {
				log.error(I18n.get("log.d4034bbb0887", serverId,
						NetworkConfig.GAMESERVER_ID));
			} else {
				byte targetServerId = readSC();
				int account = readD();
				int targetAccount = readD();
				int playerId = readD();
				int taskId = readD();
				GameRuntimeServices.playerTransferService().startTransfer(account, targetAccount, playerId, targetServerId,
						taskId);
			}
		}
			break;
		}
	}

	/**
	 * 无运行时逻辑（数据已在 readImpl 中处理）。
	 * No runtime logic (data is handled in readImpl).
	 */
	@Override
	protected void runImpl() {

	}
}
