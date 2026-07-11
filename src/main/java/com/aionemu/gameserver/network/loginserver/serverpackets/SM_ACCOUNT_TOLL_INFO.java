package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服同步账号通行点/月华余额的服务端包。
 * Server packet that syncs account toll and luna balances to the login server.
 *
 * @author xTz
 */
public class SM_ACCOUNT_TOLL_INFO extends LsServerPacket {

	private final long toll;
	private final long luna;

	private final String accountName;

	/**
	 * 构造账号通行点/月华信息包。
	 * Constructs a new account toll/luna info packet.
	 *
	 * @param toll 通行点余额 / toll balance
	 * @param luna 月华余额 / luna balance
	 * account name
	 */
	public SM_ACCOUNT_TOLL_INFO(long toll, long luna, String accountName) {
		super(0x09);
		this.accountName = accountName;
		this.toll = toll;
		this.luna = luna;
	}

	/**
	 * 写入通行点、月华与账号名。
	 * Writes toll, luna and account name.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeQ(toll);
		writeQ(luna);
		writeS(accountName);
	}
}
