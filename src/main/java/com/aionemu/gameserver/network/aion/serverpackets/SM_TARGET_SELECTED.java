package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端反馈当前选中目标的等级、HP/MP 信息的服务端包。
 * Server packet that reports the currently selected target's level and HP/MP.
 *
 * @author Dr.Nism
 */
public class SM_TARGET_SELECTED extends AionServerPacket {

	private int level;
	private int maxHp;
	private int currentHp;
	private int maxMp;
	private int currentMp;
	private int targetObjId;

	/**
	 * 根据玩家当前目标构造选中信息包。
	 * Builds the selected-target info packet from the player's current target.
	 *
	 * @param player 选中目标的玩家 / player who selected the target
	 */
	public SM_TARGET_SELECTED(Player player) {
		if (player == null) {
			return;
		}
		VisibleObject target = player.getTarget();
		if (target != null) {
			targetObjId = target.getObjectId();
		}
		if (target instanceof Creature creature) {
			level = creature.getLevel();
			maxHp = creature.getLifeStats().getMaxHp();
			currentHp = creature.getLifeStats().getCurrentHp();
			maxMp = creature.getLifeStats().getMaxMp();
			currentMp = creature.getLifeStats().getCurrentMp();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjId);
		writeH(level);
		writeD(maxHp);
		writeD(currentHp);
		writeD(maxMp);
		writeD(currentMp);
	}
}
