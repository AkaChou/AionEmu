package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

public class SM_DIRECT_PORTAL_USE_COUNT extends AionServerPacket {
	private final int startNpcObjectId;
	private final int useCount;
	private final int remainingSeconds;
	private final boolean startSide;
	private final int invadeType;
	private final int extraUseCount;

	public SM_DIRECT_PORTAL_USE_COUNT(int startNpcObjectId, int useCount, int remainingSeconds, boolean startSide,
			int invadeType, int extraUseCount) {
		this.startNpcObjectId = startNpcObjectId;
		this.useCount = useCount;
		this.remainingSeconds = remainingSeconds;
		this.startSide = startSide;
		this.invadeType = invadeType;
		this.extraUseCount = extraUseCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(3);
		writeD(startNpcObjectId);
		writeD(useCount);
		writeD(remainingSeconds);
		writeC(startSide ? 1 : 0);
		writeC(invadeType);
		writeD(extraUseCount);
	}
}
