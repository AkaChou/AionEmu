package com.aionemu.gameserver.network.loginserver.clientpackets;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.GameServerAuthFailure;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_LIST;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_GS_AUTH;

/**
 * 登录服对 SM_GS_AUTH 的响应包，通知游戏服注册是否成功及失败原因。
 * LoginServer response for SM_GS_AUTH notifying whether Gameserver registration succeeded.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class CM_GS_AUTH_RESPONSE extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_GS_AUTH_RESPONSE(int opCode) {
		super(opCode);
	}

	/**
	 * 响应码：0=已鉴权，1=鉴权失败，2=已注册。
	 * Response: 0=Authed, 1=NotAuthed, 2=AlreadyRegistered.
	 */
	private int response;

	private byte serverCount;

	/**
	 * 读取响应码及成功时的服务器数量。
	 * Reads response code and, when successful, server count.
	 */
	@Override
	public void readImpl() {
		response = readC();
		if (response == 0) {
			serverCount = (byte) readC();
		}
	}

	/**
	 * 按响应码切换连接状态、上报在线账号列表，或重试/失败退出。
	 * Switches connection state and reports online accounts, or retries/fails by response code.
	 */
	@Override
	public void runImpl() {
		/**
	 * 已认证 / Authed
	 */
		if (response == 0) {
			getConnection().setState(State.AUTHED);
			sendPacket(new SM_ACCOUNT_LIST(com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().getLoggedInAccounts()));
			GameServerNetworkServices.networkController().setServerCount(serverCount);
		}

		/**
	 * 未认证 / NotAuthed
	 */
		else if (response == 1) {
			log.error(I18n.get("log.c84f1ba0857b"));
			GameServerAuthFailure.notAuthenticated("LoginServer");
		}
		/**
	 * 已注册 / AlreadyRegistered
	 */
		else if (response == 2) {
			log.info(I18n.get("log.6902a6765b66"));
			/**
	 * 10 秒后重试 / try again after 10s
	 */
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					CM_GS_AUTH_RESPONSE.this.sendPacket(new SM_GS_AUTH());
				}
			}, 10000);
		}
	}
}
