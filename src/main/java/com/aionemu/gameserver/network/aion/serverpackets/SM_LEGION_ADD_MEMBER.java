package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端军团新增成员的服务端包。
 * Server packet notifying the client that a member has been added to the legion.
 *
 * @author Simple
 */
public class SM_LEGION_ADD_MEMBER extends AionServerPacket {

	private Player player;
	private boolean isMember;
	private int msgId;
	private String text;

	/**
	 * 构造军团新增成员通知包。
	 * Creates a packet announcing a newly added legion member.
	 *
	 * @param player 新增的玩家 / the player being added
	 * @param isMember 是否为新成员标记 / whether flagged as a new member
	 * message id
	 * @param text 附加文本 / additional text
	 */
	public SM_LEGION_ADD_MEMBER(Player player, boolean isMember, int msgId, String text) {
		this.player = player;
		this.isMember = isMember;
		this.msgId = msgId;
		this.text = text;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeS(player.getName());
		writeC(player.getLegionMember().getRank().getRankId());
		writeC(isMember ? 0x01 : 0x00);// is New Member?
		writeC(player.getCommonData().getPlayerClass().getClassId());
		writeC(player.getLevel());
		writeD(player.getPosition().getMapId());
		writeD(NetworkConfig.GAMESERVER_ID);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeD(msgId);
		writeS(text);
	}
}
