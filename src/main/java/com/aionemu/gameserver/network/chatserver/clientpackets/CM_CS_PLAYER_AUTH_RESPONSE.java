package com.aionemu.gameserver.network.chatserver.clientpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.chatserver.CsClientPacket;
import com.aionemu.gameserver.services.ChatService;

/**
 * 聊天服对玩家认证请求的应答包，携带令牌。
 * Chat-server response to a player auth request, carrying the token.
 */
@Slf4j
public class CM_CS_PLAYER_AUTH_RESPONSE extends CsClientPacket {
	/**
	 * 玩家对象 ID。
	 * Player object id.
	 */
	private int playerId;

	/**
	 * 聊天认证令牌。
	 * Chat authentication token.
	 */
	private byte[] token;

	/**
	 * 使用指定操作码构造应答包。
	 * Constructs the response packet with the given opcode.
	 *
	 * packet opcode
	 */
	public CM_CS_PLAYER_AUTH_RESPONSE(int opcode) {
		super(opcode);
	}

	/**
	 * 读取玩家 ID 与令牌字节。
	 * Reads player id and token bytes.
	 */
	@Override
	protected void readImpl() {
		playerId = readD();
		int tokenLenght = readC();
		token = readB(tokenLenght);
	}

	/**
	 * 将令牌交给 ChatService 完成玩家聊天认证。
	 * Hands the token to ChatService to complete player chat authentication.
	 */
	@Override
	protected void runImpl() {
		ChatService.playerAuthed(playerId, token);
	}
}
