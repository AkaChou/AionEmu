package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 玩家变身为召唤物形态时的同步服务端包。
 * Server packet that syncs a player transforming into a summon form.
 *
 * @author xTz
 */
public class SM_TRANSFORM_IN_SUMMON extends AionServerPacket {

	private Player player;
	private int summonObject;

	/**
	 * 玩家 / player
	 * summon creature
	 */
	public SM_TRANSFORM_IN_SUMMON(Player player, Creature creature) {
		this(player, creature.getObjectId());
	}

	/**
	 * 玩家 / player
	 * @param creatureObjectId 召唤物对象 ID / summon object id
	 */
	public SM_TRANSFORM_IN_SUMMON(Player player, int creatureObjectId) {
		this.player = player;
		this.summonObject = creatureObjectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summonObject);
		writeS(player.getName());
		writeD(player.getObjectId());
	}
}
