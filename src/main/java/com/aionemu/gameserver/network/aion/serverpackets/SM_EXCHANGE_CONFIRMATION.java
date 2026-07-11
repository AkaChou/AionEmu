package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 交易确认状态包：同步锁定/确认/取消等交易动作。
 * Exchange confirmation packet: lock/confirm/cancel action codes.
 *
 * @author -Avol-
 */
public class SM_EXCHANGE_CONFIRMATION extends AionServerPacket {

	private int action;

	public SM_EXCHANGE_CONFIRMATION(int action) {
		this.action = action;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
	}
}
