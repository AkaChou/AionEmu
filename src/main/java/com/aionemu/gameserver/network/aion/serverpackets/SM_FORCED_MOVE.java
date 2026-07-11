package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 强制位移包：将目标瞬移/拉扯到指定坐标。
 * Forced-move packet: teleports/pulls a target to given coordinates.
 *
 * @author Sweetkr
 */
public class SM_FORCED_MOVE extends AionServerPacket {

	private Creature creature;
	private int objectId;
	private float x;
	private float y;
	private float z;

	/**
	 * 将目标生物强制移动到其当前位置（通常用于拉扯同步）。
	 * Force-moves the target creature to its current position (typically for pull sync).
	 */
	public SM_FORCED_MOVE(Creature creature, Creature target) {
		this(creature, target.getObjectId(), target.getX(), target.getY(), target.getZ());
	}

	/**
	 * @param creature 发起强制位移的生物 / creature initiating the forced move
	 * target object id
	 * @param x        目标 X / dest X
	 * @param y        目标 Y / dest Y
	 * @param z        目标 Z / dest Z
	 */
	public SM_FORCED_MOVE(Creature creature, int objectId, float x, float y, float z) {
		this.creature = creature;
		this.objectId = objectId;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(creature.getObjectId());
		writeD(objectId);// targets objectId
		writeC(16); // 未知 / unk
		writeF(x);
		writeF(y);
		writeF(z);
	}
}
