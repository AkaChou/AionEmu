package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.SiegeService;
/**
 * 向客户端同步全部或单个攻城地点信息。
 * Server packet synchronizing all or a single siege location info to the client.
 */
@Slf4j

public class SM_SIEGE_LOCATION_INFO extends AionServerPacket {
	private int infoType;
	private Map<Integer, SiegeLocation> locations;

	/**
	 * 构造默认的 SM_SIEGE_LOCATION_INFO 包。
	 * Creates a default SM_SIEGE_LOCATION_INFO packet.
	 */
	public SM_SIEGE_LOCATION_INFO() {
		this.infoType = 0;
		locations = GameFeatureServices.siegeService().getSiegeLocations();
	}

	/**
	 * 使用给定参数构造 SM_SIEGE_LOCATION_INFO 包。
	 * Creates a SM_SIEGE_LOCATION_INFO packet with the given parameters.
	 *
	 * @param loc 攻城地点 / siege location
	 */
	public SM_SIEGE_LOCATION_INFO(SiegeLocation loc) {
		this.infoType = 1;
		locations = new HashMap<>();
		locations.put(loc.getLocationId(), loc);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		if (!SiegeConfig.SIEGE_ENABLED) {
			writeC(0);
			writeH(0);
			return;
		}
		writeC(infoType);
		writeH(locations.size());
		for (SiegeLocation loc : locations.values()) {
			LegionEmblem emblem = new LegionEmblem();
			writeD(loc.getLocationId());
			int legionId = loc.getLegionId();
			writeD(legionId);
			if (legionId != 0) {
				if (GameCoreGameplayServices.legionService().getLegion(legionId) == null) {
					log.error(I18n.get("log.6ea33edea84b", legionId));
				} else {
					emblem = GameCoreGameplayServices.legionService().getLegion(legionId).getLegionEmblem();
				}
			}
			if (emblem.getEmblemType() == LegionEmblemType.DEFAULT) {
				writeD(emblem.getEmblemId());
				writeC(255);
				writeC(emblem.getColor_r());
				writeC(emblem.getColor_g());
				writeC(emblem.getColor_b());
			} else {
				writeD(emblem.getCustomEmblemData().length);
				writeC(255);
				writeC(emblem.getColor_r());
				writeC(emblem.getColor_g());
				writeC(emblem.getColor_b());
			}
			writeC(loc.getRace().getRaceId());
			writeC(loc.isVulnerable() ? 2 : 0);
			writeC(loc.isCanTeleport(player) ? 1 : 0);
			writeC(loc.getNextState());
			writeH(0);
			writeH(1);
			writeD(0x00);
			writeD(0x9);
			writeD(10000);
			writeD(0x00);

			// 5.3 未知协议 / 5.3 unk protocol
			writeH(0x00);
			writeD(22597);
			writeD(0x00);
			writeH(0x00);
		}
	}
}
