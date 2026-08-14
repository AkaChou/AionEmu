package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 自身好友可见状态包：同步玩家对外的在线/隐身状态。
 * Own friend-visibility status packet: online/invisible status shown to friends.
 */
public class SM_FRIEND_STATUS extends AionServerPacket {

	/** 对好友展示的可见状态（在线/隐身）/ friend-visible status (online/invisible) */
	byte status;

	/**
	 * @param status 对好友展示的可见状态 / friend-visible status
	 */
	public SM_FRIEND_STATUS(byte status) {
		this.status = status;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(status);
	}
}
