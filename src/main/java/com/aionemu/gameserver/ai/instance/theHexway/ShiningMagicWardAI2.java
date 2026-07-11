package com.aionemu.gameserver.ai.instance.theHexway;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * The Hexway 副本 NPC AI：Shining Magic Ward（@AIName "shiningmagicward"），继承 ActionItemNpcAI2。
 * The Hexway instance NPC AI: Shining Magic Ward (@AIName "shiningmagicward"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("shiningmagicward")
public class ShiningMagicWardAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 700455: //Shining Magic Ward.
				switch (player.getWorldId()) {
                    case 300080000: //Left Wing Chamber.
					    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						   PacketSendUtility.sendMessage(player, "you enter <Primum Landing>");
						   TeleportService2.teleportTo(player, 400010000, 1071.7615f, 2851.7764f, 1636.0677f, (byte) 38);
			            } else if (player.getCommonData().getRace() == Race.ELYOS) {
						   PacketSendUtility.sendMessage(player, "you enter <Terminon Landing>");
						   TeleportService2.teleportTo(player, 400010000, 2872.6626f, 1029.0958f, 1527.9968f, (byte) 103);
					    }
			        break;
				} switch (player.getWorldId()) {
                    case 300700000: //The Hexway 4.3.
					    PacketSendUtility.sendMessage(player, "you enter in <Silentera Canyon [Master Server]>");
						TeleportService2.teleportTo(player, 600110000, 528.7647f, 766.7518f, 299.61633f, (byte) 1);
			        break;
				}
		    break;
		}
	}
}
