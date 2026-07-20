package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

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
		switch (player.getWorldId()) {
			case 300520000, 300630000 -> TeleportService2.teleportTo(player, player.getWorldId(),
				512.75183f, 515.7632f, 417.40436f, (byte) 0);
		}
	}
}
