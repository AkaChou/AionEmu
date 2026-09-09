package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 好友事件通知包：好友上线/下线/删除等事件。
 * Friend event notify packet: login, logout or deletion by a friend.
 *
 * @author Ben
 */
@AllArgsConstructor
public class SM_FRIEND_NOTIFY extends AionServerPacket {

	/** 好友上线（或变为可见） / friend logged in (or became visible) */
	public static final int LOGIN = 0;
	/** 好友下线（或变为隐身） / friend logged out (or became invisible) */
	public static final int LOGOUT = 1;
	/** 好友已将你删除 / friend deleted you */
	public static final int DELETED = 2;

	private final int code;
	private final String name;

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(name);
		writeC(code);
	}
}
