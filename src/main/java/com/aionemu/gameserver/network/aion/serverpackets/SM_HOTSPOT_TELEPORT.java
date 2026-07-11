package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端热点传送相关操作（开始、确认、取消、冷却等）的服务端包。
 * Server packet that notifies the client of hotspot teleport actions (start, confirm, cancel, cooldown, etc.).
 *
 * @author Ranastic
 */
public class SM_HOTSPOT_TELEPORT extends AionServerPacket {
	int playerObjId;
	int action;
	int teleportId;
	int cooldown;

	/**
	 * 构造仅含动作与玩家 ID 的热点传送包（动作 0/2）。
	 * Creates a hotspot teleport packet with action and player id only (actions 0/2).
	 *
	 * action type
	 * player object id
	 */
	public SM_HOTSPOT_TELEPORT(int action, int playerObjId) {
		this.action = action;
		this.playerObjId = playerObjId;
	}

	/**
	 * 构造含传送点 ID 的热点传送包（动作 1）。
	 * Creates a hotspot teleport packet with teleport id (action 1).
	 *
	 * action type
	 * player object id
	 * teleport id
	 */
	public SM_HOTSPOT_TELEPORT(int action, int playerObjId, int teleportId) {
		this.action = action;
		this.playerObjId = playerObjId;
		this.teleportId = teleportId;
	}

	/**
	 * 构造含冷却时间的热点传送包（动作 3）。
	 * Creates a hotspot teleport packet with cooldown (action 3).
	 *
	 * 玩家 / player
	 * action type
	 * teleport id
	 * cooldown
	 */
	public SM_HOTSPOT_TELEPORT(Player player, int action, int teleportId, int cooldown) {
		this.playerObjId = player.getObjectId();
		this.teleportId = teleportId;
		this.action = action;
		this.cooldown = cooldown;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 0:
			writeD(playerObjId);
			break;
		case 1:
			writeD(playerObjId);
			writeD(teleportId);
			break;
		case 2:
			writeD(playerObjId);
			break;
		case 3:
			writeD(playerObjId);
			writeD(teleportId);
			writeD(cooldown);
			break;
		}
	}
}
