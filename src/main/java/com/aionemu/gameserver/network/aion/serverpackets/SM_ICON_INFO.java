package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端显示或隐藏指定增益图标的服务端包。
 * Server packet that shows or hides a buff icon on the client.
 */
public class SM_ICON_INFO extends AionServerPacket {
	private int buffId;
	private boolean display;

	/**
	 * 使用增益 ID 与显示标志构造图标包。
	 * Creates an icon packet from a buff id and display flag.
	 *
	 * buff/icon id
	 * @param display 是否显示图标 / whether to display the icon
	 */
	public SM_ICON_INFO(int buffId, boolean display) {
		this.buffId = buffId;
		this.display = display;
	}

	protected void writeImpl(AionConnection con) {
		writeD(1);
		writeD(buffId);
		writeC(display ? 1 : 0);
	}
}
