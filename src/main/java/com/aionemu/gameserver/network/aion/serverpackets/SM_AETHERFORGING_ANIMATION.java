package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端播放奥德锻造（Aetherforging）进度条动画的服务端包。
 * Server packet playing the Aetherforging progress-bar animation on the client.
 * @author Ranastic
 */
public class SM_AETHERFORGING_ANIMATION extends AionServerPacket {
	private final int recipeId;
	private final int barTime;
	private final int type;

	/**
	 * 构造奥德锻造动画包。
	 * Creates an Aetherforging animation packet.
	 * @param recipeId 配方 ID / recipe id
	 * @param barTime 进度条时长 / progress bar duration
	 * @param type 动画/状态类型 / animation/status type
	 */
	public SM_AETHERFORGING_ANIMATION(int recipeId, int barTime, int type) {
		this.recipeId = recipeId;
		this.barTime = barTime;
		this.type = type;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeC(type);
		writeD(recipeId);
		writeD(barTime);
	}
}
