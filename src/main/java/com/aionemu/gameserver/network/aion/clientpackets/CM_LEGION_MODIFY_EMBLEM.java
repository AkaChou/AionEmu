package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 修改军团徽章颜色/类型的客户端包。
 * Client packet for modifying the legion emblem colors/type.
 *
 * @author Simple modified cura
 */
public class CM_LEGION_MODIFY_EMBLEM extends AionClientPacket {

	/** 徽章相关信息 / Emblem related information */
	private int legionId;
	private int emblemId;
	private int red;
	private int green;
	private int blue;
	private LegionEmblemType emblemType;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_MODIFY_EMBLEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		legionId = readD();
		emblemId = readC();
		emblemType = (readC() == LegionEmblemType.DEFAULT.getValue()) ? LegionEmblemType.DEFAULT
				: LegionEmblemType.CUSTOM;
		readC(); // 0xFF (Fixed)
		red = readC();
		green = readC();
		blue = readC();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();

		if (activePlayer.isLegionMember())
			GameCoreGameplayServices.legionService().storeLegionEmblem(activePlayer, legionId, emblemId, red, green, blue,
					emblemType);
	}
}
