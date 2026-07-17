package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.ArmsfusionService;

/**
 * 请求武器融合的客户端包。
 * Client packet requesting weapons fusion.
 *
 * @author zdead modified by Wakizashi
 */
public class CM_FUSION_WEAPONS extends AionClientPacket {

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_FUSION_WEAPONS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	private int npcObjId;
	private int firstItemId;
	private int secondItemId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		npcObjId = readD();
		firstItemId = readD();
		secondItemId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		ArmsfusionService.fusionWeapons(player, firstItemId, secondItemId);
	}
}
