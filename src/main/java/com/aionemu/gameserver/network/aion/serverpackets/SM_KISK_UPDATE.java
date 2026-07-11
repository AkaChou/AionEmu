package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步 Kisk 状态信息的服务端包。
 * Server packet that synchronizes Kisk status information to the client.
 */
public class SM_KISK_UPDATE extends AionServerPacket {
	private int objId;
	private int creatorid;
	private int useMask;
	private int currentMembers;
	private int maxMembers;
	private int remainingRessurects;
	private int maxRessurects;
	private int remainingLifetime;

	/**
	 * 根据 Kisk 实例构造状态同步包。
	 * Creates a Kisk status update packet from the given Kisk instance.
	 *
	 * target Kisk
	 */
	public SM_KISK_UPDATE(Kisk kisk) {
		this.objId = kisk.getObjectId();
		this.creatorid = kisk.getCreatorId();
		this.useMask = kisk.getUseMask();
		this.currentMembers = kisk.getCurrentMemberCount();
		this.maxMembers = kisk.getMaxMembers();
		this.remainingRessurects = kisk.getRemainingResurrects();
		this.maxRessurects = kisk.getMaxRessurects();
		this.remainingLifetime = kisk.getRemainingLifetime();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objId);
		writeD(creatorid);
		writeD(useMask);
		writeD(currentMembers);
		writeD(maxMembers);
		writeD(remainingRessurects);
		writeD(maxRessurects);
		writeD(remainingLifetime);
	}
}
