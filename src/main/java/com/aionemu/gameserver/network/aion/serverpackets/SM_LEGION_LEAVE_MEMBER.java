package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端军团成员离开（或被踢出）的服务端包。
 * Server packet notifying the client that a legion member has left (or been kicked).
 *
 * @author Simple
 */
public class SM_LEGION_LEAVE_MEMBER extends AionServerPacket {

	private String name;
	private String name1;
	private int playerObjId;
	private int msgId;

	/**
	 * 构造仅含单名称的成员离开通知包。
	 * Creates a leave-member packet with a single name.
	 *
	 * message id
	 * @param playerObjId 离开成员对象 ID / leaving member object id
	 * @param name 成员名称 / member name
	 */
	public SM_LEGION_LEAVE_MEMBER(int msgId, int playerObjId, String name) {
		this.msgId = msgId;
		this.playerObjId = playerObjId;
		this.name = name;
	}

	/**
	 * 构造含双名称的成员离开通知包（如踢出时同时带操作者与被踢者名称）。
	 * Creates a leave-member packet with two names (e.g. kicker and kicked).
	 *
	 * message id
	 * @param playerObjId 离开成员对象 ID / leaving member object id
	 * primary name
	 * secondary name
	 */
	public SM_LEGION_LEAVE_MEMBER(int msgId, int playerObjId, String name, String name1) {
		this.msgId = msgId;
		this.playerObjId = playerObjId;
		this.name = name;
		this.name1 = name1;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeC(0x00); // isMember ? 1 : 0
		writeD(0x00); // unix time for log off
		writeD(msgId);
		writeS(name);
		writeS(name1);
	}
}
