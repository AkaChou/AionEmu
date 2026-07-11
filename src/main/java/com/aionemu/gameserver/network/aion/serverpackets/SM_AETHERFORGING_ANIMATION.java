package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端播放以太锻造（Aetherforging）进度条动画的服务端包。
 * Server packet playing the Aetherforging progress-bar animation on the client.
 *
 * @author Ranastic
 */
public class SM_AETHERFORGING_ANIMATION extends AionServerPacket {
	private int recipeId;
	private int barTime;
	private int type;

	/**
	 * 构造以太锻造动画包。
	 * Creates an Aetherforging animation packet.
	 *
	 * @param player 执行锻造的玩家（当前未写入包体） / forging player (not written into the packet body currently)
	 * recipe id
	 * @param barTime 进度条时长 / progress bar duration
	 * @param type 动画/状态类型 / animation/status type
	 */
	public SM_AETHERFORGING_ANIMATION(Player player, int recipeId, int barTime, int type) {
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
