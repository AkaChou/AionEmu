package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端删除指定配方。
 * Server packet notifying the client to delete a recipe.
 *
 * @author namedrisk
 */
public class SM_RECIPE_DELETE extends AionServerPacket {

	private int recipeId;

	/**
	 * 使用给定参数构造 SM_RECIPE_DELETE 包。
	 * Creates a SM_RECIPE_DELETE packet with the given parameters.
	 *
	 * recipe id
	 */
	public SM_RECIPE_DELETE(int recipeId) {
		this.recipeId = recipeId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(recipeId);
	}
}
