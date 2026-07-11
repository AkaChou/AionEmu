package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ReviveType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.player.PlayerReviveService;

/**
 * 客户端复活请求包（按复活类型分发处理）。
 * Client packet for revive requests (dispatched by revive type).
 *
 * @author Ranastic (Encom)
 */
public class CM_REVIVE extends AionClientPacket {

	private int reviveId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_REVIVE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		reviveId = readC();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		if (!activePlayer.getLifeStats().isAlreadyDead()) {
			return;
		}
		ReviveType reviveType = ReviveType.getReviveTypeById(reviveId, activePlayer);
		switch (reviveType) {
		case BIND_REVIVE:
		case VORTEX_REVIVE:
			PlayerReviveService.bindRevive(activePlayer);
			break;
		case REBIRTH_REVIVE:
			PlayerReviveService.rebirthRevive(activePlayer);
			break;
		case ITEM_SELF_REVIVE:
			PlayerReviveService.itemSelfRevive(activePlayer);
			break;
		case SKILL_REVIVE:
			PlayerReviveService.skillRevive(activePlayer);
			break;
		case KISK_REVIVE:
			PlayerReviveService.kiskRevive(activePlayer);
			break;
		case INSTANCE_REVIVE:
			PlayerReviveService.instanceRevive(activePlayer);
			break;
		case START_POINT_REVIVE:
			PlayerReviveService.startPositionRevive(activePlayer);
			break;
		default:
			break;
		}
	}
}
