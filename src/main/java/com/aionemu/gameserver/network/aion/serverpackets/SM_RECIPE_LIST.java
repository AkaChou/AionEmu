package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Set;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家已学习的配方列表。
 * Server packet synchronizing the player's learned recipe list to the client.
 *
 * @author lord_rex
 */
public class SM_RECIPE_LIST extends AionServerPacket {

	private Integer[] recipeIds;
	private int count;

	/**
	 * 使用给定参数构造 SM_RECIPE_LIST 包。
	 * Creates a SM_RECIPE_LIST packet with the given parameters.
	 *
	 * recipe id set
	 */
	public SM_RECIPE_LIST(Set<Integer> recipeIds) {
		this.recipeIds = recipeIds.toArray(new Integer[recipeIds.size()]);
		this.count = recipeIds.size();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(count);
		for (int id : recipeIds) {
			writeD(id);
			writeC(0);
		}
	}
}
