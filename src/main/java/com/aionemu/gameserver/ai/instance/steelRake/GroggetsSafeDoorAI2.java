package com.aionemu.gameserver.ai.instance.steelRake;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Steel Rake 副本 NPC AI：Groggets Safe Door（@AIName "groggetssafedoor"），继承 ActionItemNpcAI2。
 * Steel Rake instance NPC AI: Groggets Safe Door (@AIName "groggetssafedoor"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("groggetssafedoor")
public class GroggetsSafeDoorAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730199: //Groggets Safe Door.
				switch (player.getWorldId()) {
                    case 300100000: //Steel Rake.
						if (player.getInventory().decreaseByItemId(185000046, 1)) { //Grogget's Safe Key.
						    PacketSendUtility.sendMessage(player, "you enter <Inside Steel Rake>");
							TeleportService2.teleportTo(player, 300100000, 702.11993f, 500.80948f, 939.60675f, (byte) 0);
						} else {
							PacketSendUtility.sendMessage(player, "you must have <Grogget's Safe Key> for use this teleporter");
						}
			        break;
				} switch (player.getWorldId()) {
                    case 300460000: //Steel Rake Cabin 3.0
					    PacketSendUtility.sendMessage(player, "you enter <Inside Steel Rake Cabin>");
						TeleportService2.teleportTo(player, 300460000, 702.11993f, 500.80948f, 939.60675f, (byte) 0);
			        break;
				}
		    break;
		}
	}
}
