package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Request;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;

@AIName("retail_direct_portal")
public class RetailDirectPortalAI2 extends ActionItemNpcAI2 {
	private static final int CANCEL_DIALOG_METERS = 10;

	@Override
	protected void handleCreatureSee(Creature creature) {
		if (creature instanceof Player player) {
			RetailDirectPortalEngine.sendUseCount(getOwner(), player);
		}
	}

	@Override
	protected void handleUseItemFinish(Player player) {
		int apCost = RetailDirectPortalEngine.extraCostFor(getOwner());
		if (apCost == 0) {
			RetailDirectPortalEngine.use(getOwner(), player);
			return;
		}
		AI2Actions.addRequest(this, player, SM_QUESTION_WINDOW.STR_ASK_PASS_BY_DIRECT_PORTAL_USE_AP,
			getOwner().getObjectId(), CANCEL_DIALOG_METERS, new AI2Request() {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					RetailDirectPortalEngine.use(getOwner(), responder, true);
				}
			}, apCost);
	}
}
