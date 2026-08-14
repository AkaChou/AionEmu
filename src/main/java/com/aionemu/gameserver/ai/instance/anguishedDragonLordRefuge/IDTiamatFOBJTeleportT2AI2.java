package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：ID Tiamat FOBJ Teleport T2（@AIName "blood_red_jewel"），继承 ActionItemNpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: ID Tiamat FOBJ Teleport T2 (@AIName "blood_red_jewel"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("blood_red_jewel")
public class IDTiamatFOBJTeleportT2AI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730625: //Blood Red Jewel.
		        // 血红宝石。 / Blood Red Jewel.
				switch (player.getWorldId()) {
					case 300520000: //Dragon Lord's Refuge 3.9
					    // 龙帝避难所 3.9。 / Dragon Lord's Refuge 3.9.
						PacketSendUtility.sendMessage(player, "you enter <Dragon Lord's Refuge 3.9>");
						TeleportService2.teleportTo(player, 300520000, 512.75183f, 515.7632f, 417.40436f, (byte) 0);
			        break;
					case 300630000: //[Anguished] Dragon Lord's Refuge 4.8
					    // [苦痛的]龙帝避难所 4.8。 / [Anguished] Dragon Lord's Refuge 4.8.
					    PacketSendUtility.sendMessage(player, "you enter <[Anguished] Dragon Lord's Refuge 4.8>");
						TeleportService2.teleportTo(player, 300630000, 512.75183f, 515.7632f, 417.40436f, (byte) 0);
			        break;
				}
		    break;
		}
	}
}
