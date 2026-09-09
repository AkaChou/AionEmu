package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端军团新增成员的服务端包。
 * Server packet notifying the client that a member has been added to the legion.
 *
 * @author Simple
 */
@AllArgsConstructor
public class SM_LEGION_ADD_MEMBER extends AionServerPacket {

	private final Player player;
	private final boolean isMember;
	private final int msgId;
	private final String text;

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
