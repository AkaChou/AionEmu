package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;

/**
 * 热点观战相关的客户端包（当前仅记录日志）。
 * Client packet related to hot spectate (currently logs only).
 *
 * @author Ranastic
 */
@Slf4j
public class CM_HOT_SPECTATE extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOT_SPECTATE(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	int unkC1;
	int unkC2;
	int unkD;

	@Override
	protected void readImpl() {
		this.unkC1 = readC();
		this.unkC2 = readC();
		this.unkD = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		log.info(I18n.get("log.f42cd5b26a19", unkC1, unkC2, unkD));
	}
}
