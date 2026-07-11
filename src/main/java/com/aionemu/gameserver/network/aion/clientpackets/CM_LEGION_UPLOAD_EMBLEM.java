package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 上传自定义军团徽章数据块的客户端包。
 * Client packet for uploading a custom legion emblem data chunk.
 *
 * @author Simple
 */
public class CM_LEGION_UPLOAD_EMBLEM extends AionClientPacket {

	/** 徽章相关信息 / Emblem related information */
	private int size;
	private byte[] data;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_UPLOAD_EMBLEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		size = readD();
		data = new byte[size];
		data = readB(size);
	}

	@Override
	protected void runImpl() {
		if (data != null && data.length > 0) {
			GameCoreGameplayServices.legionService().uploadEmblemData(getConnection().getActivePlayer(), size, data);
		}
	}
}
