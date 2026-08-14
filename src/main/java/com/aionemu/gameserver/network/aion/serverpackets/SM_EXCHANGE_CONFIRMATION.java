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

	/**
	 * 按交易动作码构造确认包。
	 * Creates an exchange confirmation packet for the given action code.
	 *
	 * @param action 动作码（锁定/确认/取消等）/ action code (lock/confirm/cancel etc.)
	 */
	public SM_EXCHANGE_CONFIRMATION(int action) {
		this.action = action;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
	}
}
