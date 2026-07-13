package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端手动移除异常状态（效果）请求包。
 * Client packet for manually removing an altered state (effect).
 *
 * @author dragoon112
 */
public class CM_REMOVE_ALTERED_STATE extends AionClientPacket {

	private int skillid;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_REMOVE_ALTERED_STATE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#readImpl()
	 */
	@Override
	protected void readImpl() {
		skillid = readH();
		readC();// 4.3
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#runImpl()
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		player.getEffectController().removeTargetStoppableEffect(skillid);
	}
}
