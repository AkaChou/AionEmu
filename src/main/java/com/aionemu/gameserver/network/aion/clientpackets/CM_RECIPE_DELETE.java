package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端删除已学配方请求包。
 * Client packet to delete a learned crafting recipe.
 */
public class CM_RECIPE_DELETE extends AionClientPacket {

	int recipeId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_RECIPE_DELETE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	protected void readImpl() {
		recipeId = readD();
	}

	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		player.getRecipeList().deleteRecipe(player, recipeId);
	}
}
