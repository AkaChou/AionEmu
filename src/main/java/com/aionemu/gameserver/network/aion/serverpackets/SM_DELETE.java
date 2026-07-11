package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 可见对象删除包：通知客户端某 AionObject 不再可见，并控制移除动画速度。
 * Informs the client that an AionObject is no longer visible, with removal animation speed.
 *
 * @author -Nemesiss-
 * @update FrozenKiller
 */
public class SM_DELETE extends AionServerPacket {

	/** 不再可见的对象 ID / object id no longer visible */
	private final int objectId;
	private final int time;

	/**
	 * @param object 将从客户端视野移除的对象 / object to despawn on the client
	 * @param time   移除动画速度；15 为特殊值 / removal animation speed; 15 is a special value
	 */
	public SM_DELETE(AionObject object, int time) {
		this.objectId = object.getObjectId();
		this.time = time;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeC(time); // removal animation speed
		writeC(time == 15 ? 0x00 : 0xFF);
	}
}
