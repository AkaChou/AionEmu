package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步军团成员状态/信息更新的服务端包。
 * Server packet that synchronizes a legion member status or info update to the client.
 *
 * @author Simple
 */
public class SM_LEGION_UPDATE_MEMBER extends AionServerPacket {

	private static final byte OFFLINE = 0x00;
	private static final byte ONLINE = 0x01;
	private Player player;
	private LegionMemberEx LM;
	private int msgId;
	private String text;
	private byte isOnline;

	/**
	 * 使用在线玩家构造成员更新包。
	 * Creates a member-update packet from an online player.
	 *
	 * member player
	 * message id
	 * @param text 附加文本 / additional text
	 */
	public SM_LEGION_UPDATE_MEMBER(Player player, int msgId, String text) {
		this.player = player;
		this.msgId = msgId;
		this.text = text;
		this.isOnline = player.isOnline() ? ONLINE : OFFLINE;
	}

	/**
	 * 使用扩展成员信息构造成员更新包。
	 * Creates a member-update packet from an extended legion member record.
	 *
	 * @param LM 扩展成员信息 / extended legion member
	 * message id
	 * @param text 附加文本 / additional text
	 */
	public SM_LEGION_UPDATE_MEMBER(LegionMemberEx LM, int msgId, String text) {
		this.LM = LM;
		this.msgId = msgId;
		this.text = text;
		this.isOnline = LM.isOnline() ? ONLINE : OFFLINE;
	}

	/**
	 * 使用玩家构造离线状态更新包。
	 * Creates an offline-status update packet from a player.
	 *
	 * member player
	 */
	public SM_LEGION_UPDATE_MEMBER(Player player) {
		this.player = player;
		this.isOnline = OFFLINE;
	}

	/**
	 * 按 Player 或 LegionMemberEx 写出成员状态与消息字段。
	 * Writes member status and message fields from either Player or LegionMemberEx.
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		if (player != null) {
			writeD(player.getObjectId());
			writeC(player.getLegionMember().getRank().getRankId());
			writeC(player.getCommonData().getPlayerClass().getClassId());
			writeC(player.getLevel());
			writeD(player.getPosition().getMapId());
			writeC(isOnline);
			writeD(player.isOnline() ? 0 : player.getLastOnline());
			writeD(NetworkConfig.GAMESERVER_ID);
			writeD(msgId);
			writeS(text);
		} else if (LM != null) {
			writeD(LM.getObjectId());
			writeC(LM.getRank().getRankId());
			writeC(LM.getPlayerClass().getClassId());
			writeC(LM.getLevel());
			writeD(LM.getWorldId());
			writeC(isOnline);
			writeD(LM.isOnline() ? 0 : LM.getLastOnline());
			writeD(NetworkConfig.GAMESERVER_ID);
			writeD(msgId);
			writeS(text);
		}
	}
}
