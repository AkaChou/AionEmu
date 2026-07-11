package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步可见对象朝向目标（Look-At）状态的服务端包。
 * Server packet that synchronizes a visible object's look-at target state.
 *
 * @author alexa026
 */
public class SM_LOOKATOBJECT extends AionServerPacket {

	private VisibleObject visibleObject;
	private int targetObjectId;
	private int heading;

	/**
	 * 根据对象当前目标构造朝向同步包。
	 * Builds a look-at packet from the object's current target.
	 *
	 * @param visibleObject 需要同步朝向的可见对象 / visible object whose facing is synced
	 */
	public SM_LOOKATOBJECT(VisibleObject visibleObject) {
		this.visibleObject = visibleObject;
		if (visibleObject.getTarget() != null) {
			this.targetObjectId = visibleObject.getTarget().getObjectId();
			this.heading = Math.abs(128 - visibleObject.getTarget().getHeading());
		} else {
			this.targetObjectId = 0;
			this.heading = visibleObject.getHeading();
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(visibleObject.getObjectId());
		writeD(targetObjectId);
		writeC(heading);
	}
}
