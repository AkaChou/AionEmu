package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * Dragon Lord Refuge 副本 NPC AI：Internal Passage In（@AIName "internal_passage_in"），继承 ActionItemNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Internal Passage In (@AIName "internal_passage_in"), extends ActionItemNpcAI2.
 *
 * @author Ranastic (Encom)
 */
@AIName("internal_passage_in")
public class Internal_Passage_InAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730673: //Internal Passage In 1
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 217.144f, 195.616f, 246.071f, (byte) 0);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 217.144f, 195.616f, 246.071f, (byte) 0);
			        break;
				}
		    break;
			case 730674: //Internal Passage In 2
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 785.866f, 197.713f, 246.071f, (byte) 0);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 785.866f, 197.713f, 246.071f, (byte) 0);
			        break;
				}
		    break;
			case 730675: //Internal Passage In 3
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 217.947f, 832.552f, 246.071f, (byte) 0);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 217.947f, 832.552f, 246.071f, (byte) 0);
			        break;
				}
		    break;
			case 730676: //Internal Passage In 4
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 779.178f, 833.055f, 246.071f, (byte) 0);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 779.178f, 833.055f, 246.071f, (byte) 0);
			        break;
				}
		    break;
		}
	}
}
