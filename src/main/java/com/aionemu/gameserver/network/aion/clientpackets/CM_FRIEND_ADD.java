package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SocialService;
import com.aionemu.gameserver.world.World;

/**
 * 请求添加好友的客户端包。
 * Client packet requesting to add a friend.
 *
 * @author Ben
 */
public class CM_FRIEND_ADD extends AionClientPacket {

	private String targetName;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_FRIEND_ADD(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		targetName = readS();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {

		final Player activePlayer = getConnection().getActivePlayer();
		final Player targetPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(targetName);

		if (targetName.equalsIgnoreCase(activePlayer.getName())) {
			// 不允许将自己加入好友列表——客户端会拦截 / Adding self to friend list not allowed - Its blocked by the client by
			// 默认，因此无需发送错误 / default, so no need to send an error
		}
		// 若离线 / if offline
		else if (targetPlayer == null) {
			sendPacket(new SM_FRIEND_RESPONSE(targetName, SM_FRIEND_RESPONSE.TARGET_OFFLINE));
		} else if (activePlayer.getFriendList().getFriend(targetPlayer.getObjectId()) != null) {
			sendPacket(new SM_FRIEND_RESPONSE(targetPlayer.getName(), SM_FRIEND_RESPONSE.TARGET_ALREADY_FRIEND));
		} else if (activePlayer.getFriendList().isFull()) {
			sendPacket(SM_SYSTEM_MESSAGE.STR_BUDDYLIST_LIST_FULL);
		} else if (activePlayer.getCommonData().getRace() != targetPlayer.getCommonData().getRace()) {
			sendPacket(new SM_FRIEND_RESPONSE(targetPlayer.getName(), SM_FRIEND_RESPONSE.TARGET_NOT_FOUND));
		} else if (targetPlayer.getFriendList().isFull()) {
			sendPacket(new SM_FRIEND_RESPONSE(targetPlayer.getName(), SM_FRIEND_RESPONSE.TARGET_LIST_FULL));
		} else if (activePlayer.getBlockList().contains(targetPlayer.getObjectId())) {
			sendPacket(new SM_FRIEND_RESPONSE(targetPlayer.getName(), SM_FRIEND_RESPONSE.TARGET_BLOCKED));
		} else if (targetPlayer.getBlockList().contains(activePlayer.getObjectId())) {
			sendPacket(SM_SYSTEM_MESSAGE.STR_YOU_EXCLUDED(targetName));
		} else // 发送请求 / Send request
		{
			RequestResponseHandler responseHandler = new RequestResponseHandler(activePlayer) {

				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (!targetPlayer.getCommonData().isOnline()) {
						sendPacket(new SM_FRIEND_RESPONSE(targetName, SM_FRIEND_RESPONSE.TARGET_OFFLINE));
					} else if (activePlayer.getFriendList().isFull() || responder.getFriendList().isFull()) {
						return;
					} else {
						SocialService.makeFriends((Player) requester, responder);
					}

				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					sendPacket(new SM_FRIEND_RESPONSE(targetName, SM_FRIEND_RESPONSE.TARGET_DENIED));

				}
			};

			boolean requested = targetPlayer.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_BUDDYLIST_ADD_BUDDY_REQUEST, responseHandler);
			// 若玩家忙碌无法询问 / If the player is busy and could not be asked
			if (!requested) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_BUDDYLIST_BUSY);
			} else {
				if (targetPlayer.getPlayerSettings().isInDeniedStatus(DeniedStatus.FRIEND)) {
					sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_FRIEND(targetPlayer.getName()));
					return;
				}
				// 向好友发送询问包 / Send question packet to buddy
				targetPlayer.getClientConnection()
						.sendPacket(new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_BUDDYLIST_ADD_BUDDY_REQUEST,
								activePlayer.getObjectId(), 0, activePlayer.getName()));
			}
		}
	}
}