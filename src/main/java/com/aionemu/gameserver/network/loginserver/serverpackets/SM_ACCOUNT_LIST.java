package com.aionemu.gameserver.network.loginserver.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服同步当前已登录账号列表的服务端包。
 * Server packet that sends the list of currently logged-in accounts to the login server.
 *
 * @author SoulKeeper
 */
public class SM_ACCOUNT_LIST extends LsServerPacket {

	/**
	 * 已加载的在线账号连接映射。
	 * Map of loaded online account connections.
	 */
	private final Map<Integer, AionConnection> accounts;

	/**
	 * 构造已登录账号列表包。
	 * Constructs a new logged-in account list packet.
	 *
	 * @param accounts 在线账号连接映射 / online account connection map
	 */
	public SM_ACCOUNT_LIST(Map<Integer, AionConnection> accounts) {
		super(0x04);
		this.accounts = accounts;
	}

	/**
	 * 写入在线账号数量及各账号名称。
	 * Writes the online account count and each account name.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accounts.size());
		for (AionConnection ac : accounts.values()) {
			writeS(ac.getAccount().getName());
		}
	}
}
