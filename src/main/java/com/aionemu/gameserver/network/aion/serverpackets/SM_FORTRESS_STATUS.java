package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.Map;

import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.SiegeService;

/**
 * 要塞/影响力全局状态包：各区域种族影响力与要塞下一状态。
 * Global fortress/influence status: regional racial influence and fortress next states.
 */
public class SM_FORTRESS_STATUS extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		Map<Integer, FortressLocation> fortresses = GameFeatureServices.siegeService().getFortresses();
		Influence inf = GameRuntimeServices.influence();
		writeC(1);
		writeD(GameFeatureServices.siegeService().getSecondsBeforeHourEnd());
		writeF(inf.getGlobalElyosInfluence());
		writeF(inf.getGlobalAsmodiansInfluence());
		writeF(inf.getGlobalBalaursInfluence());
		writeH(6);
		// ========[欧比斯]======== / ========[ABYSS]========
		writeD(400010000);
		writeF(inf.getAbyssElyosInfluence());
		writeF(inf.getAbyssAsmodiansInfluence());
		writeF(inf.getAbyssBalaursInfluence());
		// ========[贝卢斯]======== / ========[BELUS]========
		writeD(400020000);
		writeF(inf.getBelusElyosInfluence());
		writeF(inf.getBelusAsmodiansInfluence());
		writeF(inf.getBelusBalaursInfluence());
		// ========[阿斯皮达]======= / ========[ASPIDA]=======
		writeD(400040000);
		writeF(inf.getAspidaElyosInfluence());
		writeF(inf.getAspidaAsmodiansInfluence());
		writeF(inf.getAspidaBalaursInfluence());
		// =======[阿塔纳托斯]====== / =======[ATANATOS]======
		writeD(400050000);
		writeF(inf.getAtanatosElyosInfluence());
		writeF(inf.getAtanatosAsmodiansInfluence());
		writeF(inf.getAtanatosBalaursInfluence());
		// =======[迪西隆]====== / =======[DISILLON]======
		writeD(400060000);
		writeF(inf.getDisillonElyosInfluence());
		writeF(inf.getDisillonAsmodiansInfluence());
		writeF(inf.getDisillonBalaursInfluence());
		// ======[卡尔多]========= / ======[KALDOR]=========
		writeD(600090000);
		writeF(inf.getKaldorElyosInfluence());
		writeF(inf.getKaldorAsmodiansInfluence());
		writeF(inf.getKaldorBalaursInfluence());
		writeH(fortresses.size());
		for (FortressLocation fortress : fortresses.values()) {
			writeD(fortress.getLocationId());
			writeC(fortress.getNextState());
		}
	}
}
