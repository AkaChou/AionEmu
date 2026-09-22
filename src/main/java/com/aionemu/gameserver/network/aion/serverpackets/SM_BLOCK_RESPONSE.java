package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 响应黑名单相关请求的服务端包（拉黑/解黑结果等）。
 * Server packet responding to block-list related requests (block/unblock results, etc.).
 * @author Ben
 */
@AllArgsConstructor
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

	private final int code;
	private final String playerName;

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(playerName);
		writeD(code);
	}
}
