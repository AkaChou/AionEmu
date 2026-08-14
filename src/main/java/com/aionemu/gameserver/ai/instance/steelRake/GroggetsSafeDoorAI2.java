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
		// 用保险箱钥匙传送到副本内部；3.0 舱内版无需钥匙。 / Teleport inside the instance with the safe key; the 3.0 cabin version needs no key.
		switch (getNpcId()) {
		    case 730199: // 格罗盖特保险箱门 / Groggets Safe Door.
				switch (player.getWorldId()) {
                    case 300100000: // 钢铁耙号 / Steel Rake.
						if (player.getInventory().decreaseByItemId(185000046, 1)) { // 格罗盖特保险箱钥匙 / Grogget's Safe Key.
						    PacketSendUtility.sendMessage(player, "you enter <Inside Steel Rake>");
							TeleportService2.teleportTo(player, 300100000, 702.11993f, 500.80948f, 939.60675f, (byte) 0);
						} else {
							PacketSendUtility.sendMessage(player, "you must have <Grogget's Safe Key> for use this teleporter");
						}
			        break;
				} switch (player.getWorldId()) {
                    case 300460000: // 钢铁耙号舱室 3.0 / Steel Rake Cabin 3.0
					    PacketSendUtility.sendMessage(player, "you enter <Inside Steel Rake Cabin>");
						TeleportService2.teleportTo(player, 300460000, 702.11993f, 500.80948f, 939.60675f, (byte) 0);
			        break;
				}
		    break;
		}
	}
}
