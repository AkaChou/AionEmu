package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEND_EMBLEM;

/**
 * 请求发送军团徽章数据的客户端包。
 * Client packet requesting legion emblem data.
 *
 * @author Simple
 */
public class CM_LEGION_SEND_EMBLEM extends AionClientPacket {

	private int legionId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_SEND_EMBLEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		legionId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Legion legion = GameCoreGameplayServices.legionService().getLegion(legionId);

		if (legion != null) {
			LegionEmblem legionEmblem = legion.getLegionEmblem();
			if (legionEmblem.getEmblemType() == LegionEmblemType.DEFAULT) {
				sendPacket(new SM_LEGION_SEND_EMBLEM(legionId, legionEmblem.getEmblemId(), legionEmblem.getColor_r(),
						legionEmblem.getColor_g(), legionEmblem.getColor_b(), legion.getLegionName(),
						legionEmblem.getEmblemType(), 0));
			} else {
				GameCoreGameplayServices.legionService().sendEmblemData(getConnection().getActivePlayer(), legionEmblem, legionId,
						legion.getLegionName());
			}
		}
	}
}
