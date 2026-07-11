package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.SiegeService;

/**
 * 向客户端同步全局及分地图势力影响力比例的服务端包。
 * Server packet synchronizing global and per-map faction influence ratios to the client.
 */
public class SM_INFLUENCE_RATIO extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		Influence inf = GameRuntimeServices.influence();
		writeD(GameFeatureServices.siegeService().getSecondsBeforeHourEnd());
		writeF(inf.getGlobalElyosInfluence());
		writeF(inf.getGlobalAsmodiansInfluence());
		writeF(inf.getGlobalBalaursInfluence());
		writeH(2);
		// ========[欧比斯]======== / ========[ABYSS]========
		writeD(400010000);
		writeF(inf.getAbyssElyosInfluence());
		writeF(inf.getAbyssAsmodiansInfluence());
		writeF(inf.getAbyssBalaursInfluence());
		// ======[卡尔多]========= / ======[KALDOR]=========
		writeD(600090000);
		writeF(inf.getKaldorElyosInfluence());
		writeF(inf.getKaldorAsmodiansInfluence());
		writeF(inf.getKaldorBalaursInfluence());
	}
}
