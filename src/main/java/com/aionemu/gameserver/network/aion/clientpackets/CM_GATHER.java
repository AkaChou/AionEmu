package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 开始或结束采集的客户端包。
 * Client packet that starts or finishes gathering.
 *
 * @author ATracer
 */
public class CM_GATHER extends AionClientPacket {

	boolean isStartGather = false;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GATHER(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		int action = readD();
		if (action == 0) {
			isStartGather = true;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		VisibleObject target = player.getTarget();
		if (target != null && target.getPosition().isSpawned() && target instanceof Gatherable) {
			if (isStartGather) {
				((Gatherable) target).getController().onStartUse(player);
			} else {
				((Gatherable) target).getController().finishGathering(player);
			}
		}
	}
}