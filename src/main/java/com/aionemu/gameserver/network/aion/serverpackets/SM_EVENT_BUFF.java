package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 活动 Buff 同步包：向客户端写入玩家对象 ID 与活动 buff 值。
 * Event buff sync packet: player object id and event buff value.
 *
 * @author wanke
 */
public class SM_EVENT_BUFF extends AionServerPacket {
	private final Player player;
	private final int value;

	/**
	 * 构造活动 Buff 同步包。
	 * Creates an event buff sync packet.
	 *
	 * @param player 目标玩家 / target player
	 * @param id 活动 buff 值 / event buff value
	 */
	public SM_EVENT_BUFF(Player player, int id) {
		this.player = player;
		this.value = id;
	}

	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeD(value);
	}
}
