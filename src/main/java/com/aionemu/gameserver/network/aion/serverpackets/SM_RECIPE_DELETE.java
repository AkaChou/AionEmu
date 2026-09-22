package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端删除指定配方。
 * Server packet notifying the client to delete a recipe.
 * @author namedrisk
 */
@AllArgsConstructor
public class SM_RECIPE_DELETE extends AionServerPacket {

	private final int recipeId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(recipeId);
	}
}
