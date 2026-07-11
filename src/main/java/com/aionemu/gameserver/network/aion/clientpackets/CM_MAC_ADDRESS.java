package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAC_INFO;

/**
	 * 此包中客户端发送 MAC 地址。 / In this packet client is sending Mac Address - haha.
	 */

/**
 * 上报 MAC 地址与硬件标识的客户端包。
 * Client packet reporting MAC address and hardware identifier.
 *
 * @author -Nemesiss-, KID
 */
@Slf4j
public class CM_MAC_ADDRESS extends AionClientPacket {
	/**
	 * 客户端发送的 MAC，格式同 ipconfig /all。 / Mac Addres send by client in the same format as: ipconfig /all [ie: xx-xx-xx-xx-xx-xx]
	 */
	private String macAddress;
	private String HardName;
	private int localIP;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MAC_ADDRESS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取 MAC 地址、硬件名与本地 IP。
	 * Reads MAC address, hardware name, and local IP.
	 */
	@Override
	protected void readImpl() {
		readC();
		short counter = (short) readH();
		for (short i = 0; i < counter; i++)
			readD();
		macAddress = readS();
		HardName = readS();
		localIP = readD();
	}
	/**
	 * 校验 MAC 封禁后绑定地址并回包。
	 * Checks MAC ban, binds the address, and replies.
	 */
	@Override
	protected void runImpl() {
		if (GameServerNetworkServices.bannedMacManager().isBanned(macAddress)) {
			this.getConnection().closeNow();
			log.info(I18n.get("log.b98aa791e284", macAddress, this.getConnection().getIP()));
			log.info(I18n.get("log.78e831553084", HardName, HardName));
		} else {
			this.getConnection().setMacAddress(macAddress);
			sendPacket(new SM_MAC_INFO(macAddress, HardName, localIP));
		}
	}
}
