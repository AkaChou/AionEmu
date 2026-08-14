package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_NOTE;

/**
 * 客户端设置个人备注请求包。
 * Client packet for setting the player's personal note.
 *
 * @author Ben
 */
public class CM_SET_NOTE extends AionClientPacket {

	private String note;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SET_NOTE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		note = readS();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();

		if (!note.equals(activePlayer.getCommonData().getNote())) {

			activePlayer.getCommonData().setNote(note);
			activePlayer.getClientConnection().sendPacket(new SM_UPDATE_NOTE(activePlayer.getObjectId(), note));

			for (Friend friend : activePlayer.getFriendList()) // 遍历我的所有好友 / For all my friends
			{
				Player frienPlayer = friend.getPlayer();
				if (friend.isOnline() && frienPlayer != null) // 若玩家在线 / If the player is online
				{
					friend.getPlayer().getClientConnection().sendPacket(new SM_FRIEND_LIST()); // 向他发送新的好友列表 / Send him a new friend
																								// 列表 / list

				}
			}
		}
	}
}
