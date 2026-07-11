package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 响应黑名单相关请求的服务端包（拉黑/解黑结果等）。
 * Server packet responding to block-list related requests (block/unblock results, etc.).
 *
 * @author Ben
 */
public class SM_BLOCK_RESPONSE extends AionServerPacket {

	/**
	 * 已将 %0 加入黑名单。
	 * You have blocked %0.
	 */
	public static final int BLOCK_SUCCESSFUL = 0;
	/**
	 * 已将 %0 移出黑名单。
	 * You have unblocked %0.
	 */
	public static final int UNBLOCK_SUCCESSFUL = 1;
	/**
	 * 该角色不存在。
	 * That character does not exist.
	 */
	public static final int TARGET_NOT_FOUND = 2;
	/**
	 * 黑名单已满。
	 * 你的Block List已满。 / Your Block List is full.
	 */
	public static final int LIST_FULL = 3;
	/**
	 * 不能将自己加入黑名单。
	 * You cannot block yourself.
	 */
	public static final int CANT_BLOCK_SELF = 4;

	private int code;
	private String playerName;

	/**
	 * 构造黑名单请求响应包。
	 * Constructs a new block request response packet.
	 *
	 * @param code 消息代码，参见类常量 / message code — see class constants
	 * @param playerName 插入消息的参数，通常为目标玩家名 / parameters inserted into the message, usually the target player's name
	 */
	public SM_BLOCK_RESPONSE(int code, String playerName) {
		this.code = code;
		this.playerName = playerName;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(playerName);
		writeD(code);
	}
}
