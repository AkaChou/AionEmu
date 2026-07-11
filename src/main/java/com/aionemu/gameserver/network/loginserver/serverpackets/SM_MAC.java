package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服上报账号 MAC 地址的服务端包。
 * Server packet that reports an account MAC address to the login server.
 *
 * @author nrg
 */
public class SM_MAC extends LsServerPacket {

	private int accountId;
	private String address;

	/**
	 * 构造 MAC 地址上报包。
	 * Constructs a MAC address report packet.
	 *
	 * 账号 ID / account id
	 * MAC address
	 */
	public SM_MAC(int accountId, String address) {
		super(13);
		this.accountId = accountId;
		this.address = address;
	}

	/**
	 * 写入账号 ID 与 MAC 地址。
	 * Writes account id and MAC address.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accountId);
		writeS(address);
	}
}
