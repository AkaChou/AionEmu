package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_RESPONSE;
import com.aionemu.gameserver.services.SocialService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 编辑好友备注的客户端包。
 * Client packet that edits a friend's note.
 */
public class CM_FRIEND_EDIT extends AionClientPacket {
	private String playerName;
	private String notice;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_FRIEND_EDIT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		playerName = readS();
		notice = readS();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		Friend friend = activePlayer.getFriendList().getFriend(playerName);
		if (friend != null) {
			PacketSendUtility.sendPacket(activePlayer,
					new SM_FRIEND_RESPONSE(playerName, SM_FRIEND_RESPONSE.TARGET_NOTE));
			SocialService.setFriendNote(activePlayer, friend, notice);
		}
	}
}