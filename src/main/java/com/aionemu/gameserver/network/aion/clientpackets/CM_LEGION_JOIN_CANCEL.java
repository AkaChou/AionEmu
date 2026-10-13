package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.LegionService;

/**
 * 取消军团加入申请的客户端包。
 * Client packet for canceling a legion join request.
 */
public class CM_LEGION_JOIN_CANCEL extends AionClientPacket {

	private int legionId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_JOIN_CANCEL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		legionId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		LegionService.getInstance().handleJoinRequestCancel(player, legionId);
	}
}
