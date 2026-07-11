package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.DuelService;

/**
 * 请求与目标玩家决斗的客户端包。
 * Client packet requesting a duel with a target player.
 *
 * @author xavier
 */
public class CM_DUEL_REQUEST extends AionClientPacket {

	/** 决斗目标对象 ID / target object id for the duel */
	private int objectId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_DUEL_REQUEST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		objectId = readD();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		AionObject target = activePlayer.getKnownList().getObject(objectId);

		if (!CustomConfig.INSTANCE_DUEL_ENABLE && activePlayer.isInInstance()) {
			return;
		}
		if (target == null) {
			return;
		}
		if (target instanceof Player && !((Player) target).equals(activePlayer)) {
			DuelService duelService = GameGameplayServices.duelService();

			Player targetPlayer = (Player) target;

			if (duelService.isDueling(activePlayer.getObjectId())) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_DUEL_YOU_ARE_IN_DUEL_ALREADY);
				return;
			}
			if (duelService.isDueling(targetPlayer.getObjectId())) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_DUEL_PARTNER_IN_DUEL_ALREADY(target.getName()));
				return;
			}
			if (targetPlayer.getPlayerSettings().isInDeniedStatus(DeniedStatus.DUEL)) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_DUEL(targetPlayer.getName()));
				return;
			}
			duelService.onDuelRequest(activePlayer, targetPlayer);
			duelService.confirmDuelWith(activePlayer, targetPlayer);
		} else {
			sendPacket(SM_SYSTEM_MESSAGE.STR_DUEL_PARTNER_INVALID(target.getName()));
		}
	}
}