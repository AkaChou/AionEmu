package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端登录会话校验（playOk/loginOk）的客户端包。
 * Client packet for login session validation (playOk/loginOk).
 *
 * @author -Nemesiss-
 */
public class CM_L2AUTH_LOGIN_CHECK extends AionClientPacket {

	/**
	 * playOk2 为会话密钥一部分，用于安全校验（与登录服下发密钥比对）。 / playOk2 is part of session key - its used for security purposes we will check if this is the key what login server sends
	 */
	private int playOk2;
	/**
	 * playOk1 为会话密钥一部分，用于安全校验（与登录服下发密钥比对）。 / playOk1 is part of session key - its used for security purposes we will check if this is the key what login server sends
	 */
	private int playOk1;
	/**
	 * accountId 为会话密钥一部分，用于认证（校验是否匹配登录服等待账号）。 / accountId is part of session key - its used for authentication we will check if this accountId is matching any waiting account login server side and check if rest of session key is ok
	 */
	private int accountId;
	/**
	 * loginOk 为会话密钥一部分，用于安全校验（与登录服下发密钥比对）。 / loginOk is part of session key - its used for security purposes we will check if this is the key what login server sends
	 */
	private int loginOk;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_L2AUTH_LOGIN_CHECK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		playOk2 = readD();
		playOk1 = readD();
		accountId = readD();
		loginOk = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().requestAuthenticationOfClient(accountId, getConnection(), loginOk, playOk1, playOk2);
	}
}
