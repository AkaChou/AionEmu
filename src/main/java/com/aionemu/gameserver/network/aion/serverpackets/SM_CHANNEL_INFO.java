package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 同步当前地图频道信息（当前频道与实例/频道总数）的服务端包。
 * Server packet synchronizing current map channel info (current channel and total instance/channel count).
 */
public class SM_CHANNEL_INFO extends AionServerPacket {
	int instanceCount = 0;
	int currentChannel = 0;

	/**
	 * 根据玩家世界坐标构造频道信息包。
	 * Builds a channel-info packet from the player's world position.
	 *
	 * @param position 世界坐标（含地图实例） / world position (including map instance)
	 */
	public SM_CHANNEL_INFO(WorldPosition position) {
		WorldMapTemplate template = position.getWorldMapInstance().getTemplate();
		if (position.getWorldMapInstance().isBeginnerInstance()) {
			this.instanceCount = template.getBeginnerTwinCount();
			if (WorldConfig.WORLD_EMULATE_A_STATION) {
				this.instanceCount += template.getTwinCount();
			}
			this.currentChannel = position.getInstanceId() - 1;
		} else {
			this.instanceCount = template.getTwinCount();
			if (WorldConfig.WORLD_EMULATE_A_STATION) {
				this.instanceCount += template.getBeginnerTwinCount();
			}
			this.currentChannel = position.getInstanceId() - 1;
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentChannel);
		writeD(instanceCount);
	}
}
