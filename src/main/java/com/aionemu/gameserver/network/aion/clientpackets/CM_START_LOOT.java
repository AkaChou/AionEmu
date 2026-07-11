package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.drop.DropService;

/**
 * 客户端打开/关闭掉落列表请求包。
 * Client packet for opening or closing a corpse drop list.
 *
 * @author alexa026, Correted by Metos, ATracer
 */
public class CM_START_LOOT extends AionClientPacket {

	/**
	 * 目标物体 ID / Target object id that client wants to loot
	 */
	private int targetObjectId;
	private int action;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_START_LOOT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		targetObjectId = readD();// 空 / empty
		action = readC();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (action == 0) // open
		{
			GameCoreGameplayServices.dropService().requestDropList(player, targetObjectId);
		} else if (action == 1) // close
		{
			GameCoreGameplayServices.dropService().closeDropList(player, targetObjectId);
		}
	}
}
