package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MARK_FRIENDLIST;

/**
 * 请求好友列表标记数据的客户端包（房屋传送相关）。
 * Client packet requesting friend-list mark data (housing teleport related).
 */
public class CM_MARK_FRIENDLIST extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MARK_FRIENDLIST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();
		if (activePlayer != null) {
			if (!activePlayer.getFriendList().getIsFriendListSent())
				getConnection().sendPacket(new SM_FRIEND_LIST());
			getConnection().sendPacket(new SM_MARK_FRIENDLIST());
		}
	}
}
