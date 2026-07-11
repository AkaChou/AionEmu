package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.SystemMessageId;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
/**
 * 向目标玩家发起交易请求的客户端包。
 * Client packet that requests an exchange with a target player.
 */
@Slf4j

public class CM_EXCHANGE_REQUEST extends AionClientPacket {
	public Integer targetObjectId;


	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_EXCHANGE_REQUEST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();
	}

	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();
		final Player targetPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(targetObjectId);
		if (targetPlayer == null) {
			log.warn(I18n.get("log.2ed9d56579b2", activePlayer.getObjectId(), targetObjectId));
			return;
		}
		if (!activePlayer.equals(targetPlayer)) {
			if (activePlayer.getKnownList().getObject(targetPlayer.getObjectId()) == null) {
				log.info(I18n.get("log.61516bb8047e", activePlayer.getName(), targetPlayer.getName()));
				return;
			}
			if (!activePlayer.getRace().equals(targetPlayer.getRace())) {
				log.info(I18n.get("log.92dc0d1a576a", activePlayer.getName(), targetPlayer.getName()));
				return;
			}
			if (targetPlayer != null) {
				if (targetPlayer.getPlayerSettings().isInDeniedStatus(DeniedStatus.TRADE)) {
					sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_TRADE(targetPlayer.getName()));
					return;
				}
				if (targetPlayer.getInventory().isFull()) {
					// 对方携带物品过多，无法交易。 / You cannot trade with the target as the target is carrying too many items.
					PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PARTNER_TOO_HEAVY_TO_EXCHANGE);
					return;
				}
				if (activePlayer.getInventory().isFull()) {
					// 你携带物品过多，无法与对方交易。 / You cannot trade with the target as you are carrying too many items.
					PacketSendUtility.sendPacket(activePlayer,
							SM_SYSTEM_MESSAGE.STR_EXCHANGE_CANT_EXCHANGE_HEAVY_TO_ADD_EXCHANGE_ITEM);
					return;
				}
				sendPacket(SM_SYSTEM_MESSAGE.STR_EXCHANGE_ASKED_EXCHANGE_TO_HIM(targetPlayer.getName()));
				RequestResponseHandler responseHandler = new RequestResponseHandler(activePlayer) {
					@Override
					public void acceptRequest(Creature requester, Player responder) {
						GameRuntimeServices.exchangeService().registerExchange(activePlayer, targetPlayer);
					}

					@Override
					public void denyRequest(Creature requester, Player responder) {
						PacketSendUtility.sendPacket(activePlayer, new SM_SYSTEM_MESSAGE(
								SystemMessageId.EXCHANGE_HE_REJECTED_EXCHANGE, targetPlayer.getName()));
					}
				};
				boolean requested = targetPlayer.getResponseRequester()
						.putRequest(SM_QUESTION_WINDOW.STR_EXCHANGE_DO_YOU_ACCEPT_EXCHANGE, responseHandler);
				if (requested) {
					PacketSendUtility.sendPacket(targetPlayer, new SM_QUESTION_WINDOW(
							SM_QUESTION_WINDOW.STR_EXCHANGE_DO_YOU_ACCEPT_EXCHANGE, 0, 0, activePlayer.getName()));
				}
			}
		}
	}
}