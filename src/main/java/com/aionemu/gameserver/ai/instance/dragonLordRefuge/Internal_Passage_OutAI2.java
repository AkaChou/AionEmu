package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * Dragon Lord Refuge 副本 NPC AI：Internal Passage Out（@AIName "internal_passage_out"），继承 ActionItemNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Internal Passage Out (@AIName "internal_passage_out"), extends ActionItemNpcAI2.
 *
 * @author Ranastic (Encom)
 */
@AIName("internal_passage_out")
public class Internal_Passage_OutAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730633: //Internal Passage Out 1
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 530.0911f, 480.24875f, 417.40436f, (byte) 103);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 530.0911f, 480.24875f, 417.40436f, (byte) 103);
			        break;
				}
		    break;
			case 730634: //Internal Passage Out 2
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 477.32306f, 549.42285f, 417.40436f, (byte) 43);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 477.32306f, 549.42285f, 417.40436f, (byte) 43);
			        break;
				}
		    break;
			case 730635: //Internal Passage Out 3
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 530.8401f, 549.626f, 417.40436f, (byte) 17);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 530.8401f, 549.626f, 417.40436f, (byte) 17);
			        break;
				}
		    break;
			case 730636: //Internal Passage Out 4
				switch (player.getWorldId()) {
					case 300520000:
						TeleportService2.teleportTo(player, 300520000, 504.3792f, 520.4297f, 417.40436f, (byte) 61);
			        break;
					case 300630000:
						TeleportService2.teleportTo(player, 300630000, 504.3792f, 520.4297f, 417.40436f, (byte) 61);
			        break;
				}
		    break;
		}
	}
}
