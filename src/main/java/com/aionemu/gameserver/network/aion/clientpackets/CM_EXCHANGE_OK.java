package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.ExchangeService;

/**
 * 确认完成交易的客户端包。
 * Client packet that confirms and completes the exchange.
 *
 * @author -Avol-
 */
public class CM_EXCHANGE_OK extends AionClientPacket {

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_EXCHANGE_OK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {

	}

	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();
		GameRuntimeServices.exchangeService().confirmExchange(activePlayer);
	}
}