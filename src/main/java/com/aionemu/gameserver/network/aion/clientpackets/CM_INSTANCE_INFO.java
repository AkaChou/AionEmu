package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求副本冷却/队伍副本信息的客户端包。
 * Client packet requesting instance cooldown or team instance info.
 */
@Slf4j

public class CM_INSTANCE_INFO extends AionClientPacket {

	@SuppressWarnings("unused")
	private int unk1, unk2;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_INSTANCE_INFO(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		unk1 = readD();
		unk2 = readC();
	}

	@Override
	protected void runImpl() {
		if (unk2 == 1 && !getConnection().getActivePlayer().isInTeam()) {
			log.debug("Received CM_INSTANCE_INFO with teamdata request but player has no team!");
		}
		if (unk2 == 1) {
			Player player = getConnection().getActivePlayer();
			if (player.isInAlliance2()) {
				boolean answer = true;
				for (Player p : player.getPlayerAlliance2().getMembers()) {
					if (answer) {
						PacketSendUtility.sendPacket(p, new SM_INSTANCE_INFO(p, true, p.getCurrentTeam()));
						answer = false;
					} else {
						PacketSendUtility.sendPacket(p, new SM_INSTANCE_INFO(p, false, p.getCurrentTeam()));
					}
				}
			} else if (player.isInGroup2()) {
				boolean answer = true;
				for (Player p : player.getPlayerGroup2().getMembers()) {
					if (answer) {
						PacketSendUtility.sendPacket(p, new SM_INSTANCE_INFO(p, true, p.getCurrentTeam()));
						answer = false;
					} else {
						PacketSendUtility.sendPacket(p, new SM_INSTANCE_INFO(p, false, p.getCurrentTeam()));
					}
				}
			}
		} else {
			sendPacket(new SM_INSTANCE_INFO(getConnection().getActivePlayer(), true,
					getConnection().getActivePlayer().getCurrentTeam()));
		}
	}
}
