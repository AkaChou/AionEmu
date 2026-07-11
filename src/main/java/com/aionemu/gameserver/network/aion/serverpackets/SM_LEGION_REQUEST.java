package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 回复客户端军团加入申请处理结果的服务端包。
 * Server packet replying with the result of a legion join-request decision.
 */
public class SM_LEGION_REQUEST extends AionServerPacket {
	private int requesterId;
	private boolean allowed;

	/**
	 * 构造军团申请处理结果包。
	 * Creates a packet for a legion request decision result.
	 *
	 * @param requesterId 申请者对象 ID / requester object id
	 * @param allowed 是否允许加入 / whether the request is allowed
	 */
	public SM_LEGION_REQUEST(int requesterId, boolean allowed) {
		this.requesterId = requesterId;
		this.allowed = allowed;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(requesterId);
		writeC(allowed ? 1 : 0);
	}
}
