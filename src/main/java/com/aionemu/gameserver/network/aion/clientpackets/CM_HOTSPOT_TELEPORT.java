package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOTSPOT_TELEPORT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 热点传送（付费快速传送）的客户端包。
 * Client packet for hotspot (paid quick) teleport.
 *
 * @author Ranastic
 */
public class CM_HOTSPOT_TELEPORT extends AionClientPacket {
	private int action;
	private int teleportId;
	private int price;
	@SuppressWarnings("unused")
	private int unk;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOTSPOT_TELEPORT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readC();
		if (action == 1) {
			teleportId = readD();
			price = readD();
			unk = readD();
		} else if (action == 2) {
		}
	}

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		if (player.getInventory().getKinah() < price) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOTSPOT_NOT_ENOUGH_COST);
			return;
		}
		if (action == 1) {
			GameWorldBootstrapServices.hotspotTeleportService().doTeleport(player, teleportId, price);
		} else if (action == 2) {
			player.getController().cancelTask(TaskId.HOTSPOT_TELEPORT);
			PacketSendUtility.broadcastPacketAndReceive(player, new SM_HOTSPOT_TELEPORT(2, player.getObjectId()));
		}
	}
}
