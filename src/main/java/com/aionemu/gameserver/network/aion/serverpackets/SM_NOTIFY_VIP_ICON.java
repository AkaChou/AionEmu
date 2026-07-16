package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

public class SM_NOTIFY_VIP_ICON extends AionServerPacket {

	private static final int DEFAULT_VIP_ICON_ID = 3;

	private final int objectId;
	private final int vipIconId;

	public SM_NOTIFY_VIP_ICON(Player player) {
		this(player.getObjectId(), player.getPlayerAccount().getVipRemainingSeconds() > 0);
	}

	SM_NOTIFY_VIP_ICON(int objectId, boolean activeVip) {
		this.objectId = objectId;
		this.vipIconId = activeVip ? DEFAULT_VIP_ICON_ID : 0;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeH(vipIconId);
	}
}
