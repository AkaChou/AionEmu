package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步目标等级变化的服务端包。
 * Server packet that synchronizes a target's level change to the client.
 *
 * @author ATracer
 */
public class SM_LEVEL_UPDATE extends AionServerPacket {

	private int targetObjectId;
	private int effect;
	private int level;

	/**
	 * 使用目标对象、效果与等级构造等级更新包。
	 * Creates a level-update packet from target, effect and level values.
	 *
	 * target object id
	 * effect value
	 * new level
	 */
	public SM_LEVEL_UPDATE(int targetObjectId, int effect, int level) {
		this.targetObjectId = targetObjectId;
		this.effect = effect;
		this.level = level;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjectId);
		writeH(effect); // 未知 / unk
		writeH(level);
		writeH(0x00); // 未知 / unk
	}
}
