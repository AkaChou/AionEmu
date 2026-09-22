package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 强制位移包：将目标瞬移/拉扯到指定坐标。
 * Forced-move packet: teleports/pulls a target to given coordinates.
 * @author Sweetkr
 */
@AllArgsConstructor
public class SM_FORCED_MOVE extends AionServerPacket {

	private final Creature creature;
	private final int objectId;
	private final float x;
	private final float y;
	private final float z;

	/**
	 * 将目标生物强制移动到其当前位置（通常用于拉扯同步）。
	 * Force-moves the target creature to its current position (typically for pull sync).
	 */
	public SM_FORCED_MOVE(Creature creature, Creature target) {
		this(creature, target.getObjectId(), target.getX(), target.getY(), target.getZ());
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
