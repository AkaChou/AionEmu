package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 真实直达传送门 NPC AI：使用后经传送门引擎传送玩家。
 * Retail direct-portal NPC AI that teleports players through the portal engine on use.
 */
@AIName("retail_direct_portal")
public class RetailDirectPortalAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		RetailDirectPortalEngine.use(getOwner(), player);
	}
}
