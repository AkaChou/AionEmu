package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服下发账号/IP 封禁（或解封）指令的服务端包。
 * Universal server packet for account/IP ban or unban requests sent to the login server.
 *
 * @author Watson
 */
public class SM_BAN extends LsServerPacket {

	/**
	 * 封禁类型：1=账号，2=IP，3=账号+IP 全封。
	 * Ban type: 1 = account, 2 = IP, 3 = full ban (account and IP).
	 */
	private final byte type;

	/**
	 * 待封禁的账号 ID。
	 * Account to ban.
	 */
	private final int accountId;

	/**
	 * 待封禁的 IP 或掩码。
	 * IP or mask to ban.
	 */
	private final String ip;

	/**
	 * 封禁时长（分钟）。0=永久；time &lt; 0 表示解封。
	 * Duration in minutes. 0 = infinity; if time &lt; 0 then it is an unban command.
	 */
	private final int time;

	/**
	 * 发起封禁的管理员对象 ID。
	 * Object id of the admin who requested the ban.
	 */
	private final int adminObjId;

	/**
	 * 构造封禁/解封控制包。
	 * Constructs a new ban/unban control packet.
	 *
	 * @param type 封禁类型 / ban type
	 * @param accountId 账号 ID / account id
	 * @param ip IP 或掩码 / IP or mask
	 * @param time 时长（分钟）/ duration in minutes
	 * @param adminObjId 管理员对象 ID / admin object id
	 */
	public SM_BAN(byte type, int accountId, String ip, int time, int adminObjId) {
		super(0x06);
		this.type = type;
		this.accountId = accountId;
		this.ip = ip;
		this.time = time;
		this.adminObjId = adminObjId;
	}

	/**
	 * 写入封禁类型、账号、IP、时长与管理员 ID。
	 * Writes ban type, account, IP, duration and admin id.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(type);
		writeD(accountId);
		writeS(ip);
		writeD(time);
		writeD(adminObjId);
	}
}
