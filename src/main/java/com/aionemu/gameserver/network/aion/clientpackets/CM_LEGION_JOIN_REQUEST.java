package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 提交军团加入申请的客户端包。
 * Client packet for submitting a legion join request.
 */
public class CM_LEGION_JOIN_REQUEST extends AionClientPacket {
	private String legionName;
	private String joinRequestMsg;
	private int legionId;
	private int joinType;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_JOIN_REQUEST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		legionId = readD();
		legionName = readS();
		joinType = readC();
		joinRequestMsg = readS();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		GameCoreGameplayServices.legionService().handleLegionJoinRequest(player, legionId, joinType, joinRequestMsg);
	}
}
