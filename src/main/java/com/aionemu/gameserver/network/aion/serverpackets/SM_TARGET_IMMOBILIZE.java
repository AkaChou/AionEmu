package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 目标定身/定帧时同步其坐标与朝向的服务端包。
 * Server packet that syncs a target creature's position and heading when immobilized.
 * <p>
 * 对非玩家实体在启用地理数据时会校正 Z 轴，减少怪物悬空。
 * For non-player entities, corrects Z via geo data when enabled to reduce floating mobs.
 *
 * @author Sweetkr
 */
public class SM_TARGET_IMMOBILIZE extends AionServerPacket {

	private Creature creature;

	/**
	 * @param creature 被定身的生物 / immobilized creature
	 */
	public SM_TARGET_IMMOBILIZE(Creature creature) {
		this.creature = creature;
	}

	// 修改（Aion Reconstruction Project - Yoress）：为非玩家实体增加 geoZ 检测，避免浮空怪。 / modified (Aion Reconstruction Project - Yoress) - Added geoZ check for non player entities to avoid floating mobs.
	// 眩晕时更新怪物高度检测（减少怪物看起来浮空的情况）。 / and update check to mob altitude when they are stunned (mobs should appear to float in the air less often).
	@Override
	protected void writeImpl(AionConnection con) {
		if (!(creature instanceof Player)) {
			if (GeoDataConfig.GEO_ENABLE && creature.getGameStats().checkGeoNeedUpdate()) {
				float z = GameWorldServices.geoService().getZ(creature.getWorldId(), creature.getX(), creature.getY(), creature.getZ(), 0.0F, creature.getInstanceId());
				creature.setXYZH(null, null, z, null);
			}
		}
		GameServerNetworkServices.packetLoggerService().logPacketSM(this.getPacketName());
		writeD(creature.getObjectId());
		writeF(creature.getX());
		writeF(creature.getY());
		writeF(creature.getZ());
		writeC(creature.getHeading());
	}
}
