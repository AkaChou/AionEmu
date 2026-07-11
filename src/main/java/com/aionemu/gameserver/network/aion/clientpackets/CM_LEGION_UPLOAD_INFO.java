package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 上传自定义军团徽章元信息的客户端包。
 * Client packet for uploading custom legion emblem metadata.
 *
 * @author Simple modified cura
 */
public class CM_LEGION_UPLOAD_INFO extends AionClientPacket {

	/** 徽章相关信息 / Emblem related information */
	private int totalSize;
	private int red;
	private int green;
	private int blue;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_UPLOAD_INFO(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		totalSize = readD();
		readC(); // 0xFF
		red = readC();
		green = readC();
		blue = readC();
	}

	@Override
	protected void runImpl() {
		final Player activePlayer = getConnection().getActivePlayer();

		GameCoreGameplayServices.legionService().uploadEmblemInfo(activePlayer, totalSize, red, green, blue,
				LegionEmblemType.CUSTOM);
	}
}
