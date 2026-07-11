package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 好友操作响应包：添加/拒绝/已满等结果码与目标名。
 * Friend operation response: result code (added, denied, full, …) and target name.
 */
public class SM_FRIEND_RESPONSE extends AionServerPacket {
	/** 好友添加成功 / friend successfully added */
	public static final int TARGET_ADDED = 0x00;
	/** 目标离线 / target offline */
	public static final int TARGET_OFFLINE = 0x01;
	/** 已是好友 / already a friend */
	public static final int TARGET_ALREADY_FRIEND = 0x02;
	/** 目标不存在 / target not found */
	public static final int TARGET_NOT_FOUND = 0x03;
	/** 对方拒绝 / target denied */
	public static final int TARGET_DENIED = 0x04;
	/** 对方好友列表已满 / target friend list full */
	public static final int TARGET_LIST_FULL = 0x05;
	/** 已从列表移除 / removed from list */
	public static final int TARGET_REMOVED = 0x06;
	/** 目标在黑名单中 / target is blocked */
	public static final int TARGET_BLOCKED = 0x08;
	/** 目标已死亡，暂不可加好友 / target dead */
	public static final int TARGET_DEAD = 0x09;
	/** 好友备注更新 / friend note update */
	public static final int TARGET_NOTE = 0x21;
	/** 离线添加相关 / offline-add related */
	public static final int FRIEND_LOGOUT_ADD = 0x11;

	private final String player;
	private final int code;

	/**
	 * @param playerName  目标玩家名 / target player name
	 * result code
	 */
	public SM_FRIEND_RESPONSE(String playerName, int messageType) {
		player = playerName;
		code = messageType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(player);
		writeC(code);
	}
}
