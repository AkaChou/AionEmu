package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端广播玩家欧比斯军衔相关外观/状态更新的服务端包。
 * Server packet broadcasting Abyss-rank related visual/status updates for a player to the client.
 *
 * @author Nemiroff
 */
public class SM_ABYSS_RANK_UPDATE extends AionServerPacket {

	private Player player;
	private int action;

	/**
	 * 按动作类型构造军衔外观/状态更新包。
	 * Creates a rank visual/status update packet for the given action type.
	 *
	 * @param action 动作类型：0=军衔、1=固定值、2=导师状态 / action type: 0=rank, 1=fixed value, 2=mentor status
	 * target player
	 */
	public SM_ABYSS_RANK_UPDATE(int action, Player player) {
		this.action = action;
		this.player = player;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		writeD(player.getObjectId());
		switch (action) {
		case 0:
			writeD(player.getAbyssRank().getRank().getId());
			break;
		case 1:
			writeD(1263375);
			break;
		case 2:
			if (player.isMentor()) {
				writeD(1);
			} else {
				writeD(0);
			}
			break;
		}
	}
}
