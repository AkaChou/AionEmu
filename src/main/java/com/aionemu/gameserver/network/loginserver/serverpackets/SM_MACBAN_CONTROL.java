package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服下发 MAC 封禁控制指令的服务端包。
 * Server packet that sends MAC-ban control commands to the login server.
 *
 * @author KID
 */
public class SM_MACBAN_CONTROL extends LsServerPacket {

	private byte type;
	private String address;
	private String details;
	private long time;

	/**
	 * 构造 MAC 封禁控制包。
	 * Constructs a MAC-ban control packet.
	 *
	 * @param type 控制类型 / control type
	 * MAC address
	 * @param time 时间戳/时长 / timestamp or duration
	 * detail notes
	 */
	public SM_MACBAN_CONTROL(byte type, String address, long time, String details) {
		super(10);
		this.type = type;
		this.address = address;
		this.time = time;
		this.details = details;
	}

	/**
	 * 写入控制类型、MAC 地址、备注与时间。
	 * Writes control type, MAC address, details and time.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(type);
		writeS(address);
		writeS(details);
		writeQ(time);
	}
}
