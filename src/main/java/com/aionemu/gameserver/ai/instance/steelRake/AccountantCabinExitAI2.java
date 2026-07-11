package com.aionemu.gameserver.ai.instance.steelRake;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Steel Rake 副本 NPC AI：Accountant Cabin Exit（@AIName "accountant_cabin_exit"），继承 ActionItemNpcAI2。
 * Steel Rake instance NPC AI: Accountant Cabin Exit (@AIName "accountant_cabin_exit"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("accountant_cabin_exit")
public class AccountantCabinExitAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730766: //Accountant's Cabin Exit.
				switch (player.getWorldId()) {
					case 300100000: //Steel Rake 1.5
					    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						    PacketSendUtility.sendMessage(player, "you enter <Beluslan>");
						    TeleportService2.teleportTo(player, 220040000, 2685.6497f, 998.47107f, 378.00754f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
			            } else if (player.getCommonData().getRace() == Race.ELYOS) {
						    PacketSendUtility.sendMessage(player, "you enter <Heiron>");
							TeleportService2.teleportTo(player, 210040000, 1246.5492f, 2557.4895f, 138.75f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
						}
			        break;
					case 300460000: //Steel Rake Cabin 3.0
					    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						    PacketSendUtility.sendMessage(player, "you enter <Beluslan>");
						    TeleportService2.teleportTo(player, 220040000, 2685.6497f, 998.47107f, 378.00754f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
			            } else if (player.getCommonData().getRace() == Race.ELYOS) {
						    PacketSendUtility.sendMessage(player, "you enter <Heiron>");
							TeleportService2.teleportTo(player, 210040000, 1246.5492f, 2557.4895f, 138.75f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
						}
			        break;
				}
		    break;
		}
	}
}
