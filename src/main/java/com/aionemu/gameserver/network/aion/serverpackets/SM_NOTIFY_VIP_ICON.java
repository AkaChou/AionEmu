package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

public class SM_NOTIFY_VIP_ICON extends AionServerPacket {

	private final int objectId;

	public SM_NOTIFY_VIP_ICON(Player player) {
		this(player.getObjectId());
	}

	SM_NOTIFY_VIP_ICON(int objectId) {
		this.objectId = objectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeH(0);
	}
}
