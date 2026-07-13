package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@AIName("retail_direct_portal")
public class RetailDirectPortalAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		RetailDirectPortalEngine.use(getOwner(), player);
	}
}
