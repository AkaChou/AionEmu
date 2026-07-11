package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.player.PlayerEnterWorldService;

/**
 * 请求进入游戏世界（开始游玩）的客户端包。
 * Client packet requesting to enter the game world and start playing.
 *
 * @author -Nemesiss-, Avol
 */
public class CM_ENTER_WORLD extends AionClientPacket {

	/** 进入世界玩家的对象 ID / object id of the player entering the world */
	private int objectId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_ENTER_WORLD(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		objectId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {

		final AionConnection client = getConnection();
		PlayerEnterWorldService.startEnterWorld(objectId, client);
	}
}