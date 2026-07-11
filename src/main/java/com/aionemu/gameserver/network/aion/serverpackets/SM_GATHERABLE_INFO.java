package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送可采集物（或静态门）生成信息的服务端包。
 * Server packet that sends spawn info for a gatherable object (or static door) to the client.
 *
 * @author ATracer
 */
public class SM_GATHERABLE_INFO extends AionServerPacket {

	private VisibleObject visibleObject;

	/**
	 * @param visibleObject 可采集物或相关可见对象 / Gatherable or related visible object
	 */
	public SM_GATHERABLE_INFO(VisibleObject visibleObject) {
		super();
		this.visibleObject = visibleObject;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeF(visibleObject.getX());
		writeF(visibleObject.getY());
		writeF(visibleObject.getZ());
		writeD(visibleObject.getObjectId());
		writeD(visibleObject.getSpawn().getEntityId());
		writeD(visibleObject.getObjectTemplate().getTemplateId());
		if (visibleObject instanceof StaticDoor) {
			if (((StaticDoor) visibleObject).isOpen()) {
				writeH(0x09);
			} else {
				writeH(0x0A);
			}
		} else {
			writeH(1);
		}
		writeC(visibleObject.getSpawn().getHeading());
		writeD(visibleObject.getObjectTemplate().getNameId());
		writeH(0);
		writeH(0);
		writeH(0);
		writeC(100); // 未知 / unk
	}
}
