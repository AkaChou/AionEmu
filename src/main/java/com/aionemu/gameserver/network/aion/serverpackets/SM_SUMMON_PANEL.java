package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端展示召唤物控制面板。
 * Server packet displaying the summon control panel on the client.
 * @author ATracer, xTz
 */
@AllArgsConstructor
public class SM_SUMMON_PANEL extends AionServerPacket {

	private final Summon summon;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summon.getObjectId());
		writeH(summon.getLevel());
		writeD(0);// 未知 / unk
		writeD(0);// 未知 / unk
		writeD(summon.getLifeStats().getCurrentHp());
		writeD(summon.getGameStats().getMaxHp().getCurrent());
		writeD(summon.getGameStats().getMainHandPAttack().getCurrent());
		writeH(summon.getGameStats().getPDef().getCurrent());
		writeH(0);
		writeH(summon.getGameStats().getMResist().getCurrent());
		writeH(0);// 未知 / unk
		writeH(0);// 未知 / unk
		writeD(summon.getLiveTime());
	}
}
