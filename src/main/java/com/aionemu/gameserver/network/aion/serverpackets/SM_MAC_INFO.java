package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 客户端 MAC/硬件信息同步的服务端包。
 * Server packet that carries client MAC/hardware information.
 *
 * @author Ranastic
 */
public class SM_MAC_INFO extends AionServerPacket {

	private String macAddress;
	private String hardName;
	private int localIP;

	/**
	 * 构造 MAC/硬件信息包。
	 * Builds a MAC/hardware info packet.
	 *
	 * MAC address
	 * hardware name
	 * local IP as int
	 */
	public SM_MAC_INFO(String macAddress, String hardName, int localIP) {
		this.macAddress = macAddress;
		this.hardName = hardName;
		this.localIP = localIP;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(macAddress);
		writeS(hardName);
		writeD(localIP);
	}
}
