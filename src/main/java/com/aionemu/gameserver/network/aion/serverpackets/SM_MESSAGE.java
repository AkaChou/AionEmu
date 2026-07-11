package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 聊天消息服务端包。
 * Server packet that delivers a chat message to the client.
 * <p>
 * 支持多种聊天频道（普通、喊话、组队、军团、密语、联盟等），并按种族/权限控制可读性。
 * Supports multiple chat channels (normal, shout, group, legion, whisper, league, etc.)
 * and gates readability by race/access level.
 */
public class SM_MESSAGE extends AionServerPacket {
	private Player player;
	private int senderObjectId;
	private String message;
	private String senderName;
	private Race race;
	private ChatType chatType;
	private float x;
	private float y;
	private float z;

	/**
	 * 由玩家对象构造聊天消息包（含坐标，用于喊话等）。
	 * Builds a chat message from a player entity (includes coordinates for shout, etc.).
	 *
	 * sender player
	 * message body
	 * @param chatType 聊天频道类型 / chat channel type
	 */
	public SM_MESSAGE(Player player, String message, ChatType chatType) {
		this.player = player;
		this.senderObjectId = player.getObjectId();
		this.senderName = player.getName();
		this.message = message;
		this.race = player.getRace();
		this.chatType = chatType;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	/**
	 * 由原始字段构造聊天消息包（系统消息、无玩家实体场景）。
	 * no player entity). / no player entity).
	 *
	 * @param senderObjectId 发送者对象 ID / sender object id
	 * @param senderName 发送者名称 / sender display name
	 * message body
	 * @param chatType 聊天频道类型 / chat channel type
	 */
	public SM_MESSAGE(int senderObjectId, String senderName, String message, ChatType chatType) {
		this.senderObjectId = senderObjectId;
		this.senderName = senderName;
		this.message = message;
		this.chatType = chatType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		boolean canRead = true;
		if (race != null) {
			canRead = chatType.isSysMsg() || CustomConfig.SPEAKING_BETWEEN_FACTIONS || player.getAccessLevel() > 0
					|| (con.getActivePlayer() != null && con.getActivePlayer().getAccessLevel() > 0);
		}
		writeC(chatType.toInteger());
		writeC(canRead ? 0 : race.getRaceId() + 1);
		writeD(senderObjectId);
		switch (chatType) {
		case NORMAL:
		case GOLDEN_YELLOW:
		case WHITE:
		case YELLOW:
		case BRIGHT_YELLOW:
		case WHITE_CENTER:
		case YELLOW_CENTER:
		case BRIGHT_YELLOW_CENTER:
			writeH(0x00);
			writeS(message);
			break;
		case SHOUT:
			writeS(senderName);
			writeS(message);
			writeF(x);
			writeF(y);
			writeF(z);
			break;
		case ALLIANCE:
		case GROUP:
		case GROUP_LEADER:
		case LEGION:
		case WHISPER:
		case LEAGUE:
		case LEAGUE_ALERT:
		case CH1:
		case CH2:
		case CH3:
		case CH4:
		case CH5:
		case CH6:
		case CH7:
		case CH8:
		case CH9:
		case CH10:
		case COMMAND:
		case ANNOUNCE:
			writeS(senderName);
			writeS(message);
			break;
		}
	}
}
