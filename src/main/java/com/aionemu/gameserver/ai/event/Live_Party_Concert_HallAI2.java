package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Request;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Live Party Concert Hall（@AIName "lpch"），继承 NpcAI2。
 * Event NPC AI: Live Party Concert Hall (@AIName "lpch"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("lpch")
public class Live_Party_Concert_HallAI2 extends NpcAI2
{
	private final int CANCEL_DIALOG_METERS = 10;
	
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 10) {
			AI2Actions.addRequest(this, player, SM_QUESTION_WINDOW.STR_ASK_PASS_BY_EVENT_DIRECT_PORTAL, getOwner().getObjectId(), CANCEL_DIALOG_METERS, new AI2Request() {
				private boolean decisionTaken = false;
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (!decisionTaken) {
						switch (getNpcId()) {
							case 831592: //Live Party Concert Hall 4.3
								TeleportService2.teleportToInstance(responder, 600080000, 1507.4276f, 1484.457f,
										565.8799f, (byte) 16, TeleportAnimation.BEAM_ANIMATION);
							break;
						}
						decisionTaken = true;
					}
				}
				@Override
				public void denyRequest(Creature requester, Player responder) {
					decisionTaken = true;
				}
			});
		} else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
		}
	}
	
	@Override
	protected void handleDialogFinish(Player player) {
	}
}
