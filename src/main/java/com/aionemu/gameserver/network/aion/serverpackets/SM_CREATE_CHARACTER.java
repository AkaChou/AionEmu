package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.PlayerInfo;

/**
 * 角色创建响应包：返回创建结果码；成功时附带新建角色的完整账号数据。
 * Response to CM_CREATE_CHARACTER: result code, and full account data of the new character on success.
 *
 * @author Nemesiss, AEJTester
 */
public class SM_CREATE_CHARACTER extends PlayerInfo {

	/** 若响应成功 / If response is ok */
	public static final int RESPONSE_OK = 0x00;

	public static final int FAILED_TO_CREATE_THE_CHARACTER = 1;
	/** 因世界库错误创建角色失败 / Failed to create the character due to world db error */
	public static final int RESPONSE_DB_ERROR = 2;
	/** 角色数量超过服务器上限 / The number of characters exceeds the maximum allowed for the server */
	public static final int RESPONSE_SERVER_LIMIT_EXCEEDED = 4;
	/** 无效角色名 / Invalid character name */
	public static final int RESPONSE_INVALID_NAME = 5;
	/** 名称包含禁用词 / The name includes forbidden words */
	public static final int RESPONSE_FORBIDDEN_CHAR_NAME = 9;
	/** 该角色名已存在 / A character with that name already exists */
	public static final int RESPONSE_NAME_ALREADY_USED = 10;
	/** 名称已被保留 / The name is already reserved */
	public static final int RESPONSE_NAME_RESERVED = 11;
	/** 无法在同一服务器创建其他种族角色 / You cannot create characters of other races in the same server */
	public static final int RESPONSE_OTHER_RACE = 12;
	public static final int RESPONSE_CREATE_NEW = 22;

	/**
	 * response code
	 */
	private final int responseCode;

	/**
	 * Newly created player
	 */
	private final PlayerAccountData player;

	/**
	 * 构造角色创建响应。
	 * Constructs a character-creation response.
	 *
	 * @param accPlData    新建角色的账号数据（成功时写入） / new character account data (written on success)
	 * @param responseCode 结果码（成功、重名、禁用名等） / result code (ok, name taken, forbidden, …)
	 */
	public SM_CREATE_CHARACTER(PlayerAccountData accPlData, int responseCode) {
		this.player = accPlData;
		this.responseCode = responseCode;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(responseCode);

		if (responseCode == RESPONSE_OK) {
			writePlayerInfo(player); // if everything is fine, all the character's data should be sent
			writeB(new byte[32]);
			writeB(new byte[88]); // unk 4.5.0.19
		} else {
			writeB(new byte[448 + /* 4.5.0.19 unk */88]); // if something is wrong, only return code should be sent in
															// 该数据包 / the packet
		}
	}
}
