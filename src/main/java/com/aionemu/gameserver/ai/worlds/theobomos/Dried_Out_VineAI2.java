package com.aionemu.gameserver.ai.worlds.theobomos;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Theobomos 区域 NPC AI：Dried Out Vine（@AIName "dried_out_vine"），继承 ActionItemNpcAI2。
 * Theobomos zone NPC AI: Dried Out Vine (@AIName "dried_out_vine"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("dried_out_vine")
public class Dried_Out_VineAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		if (player.getLevel() >= 45) {
		    switch (getNpcId()) {
			    case 730169: //Dried-Out Vine.
				    TeleportService2.teleportTo(player, 210060000, 1971f, 2683.87f, 61.5f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
				break;
				case 730170: //Dried-Out Vine.
				    TeleportService2.teleportTo(player, 210060000, 2267.14f, 2845.41f, 57f, (byte) 31, TeleportAnimation.BEAM_ANIMATION);
				break;
				case 730171: //Dried-Out Vine.
				    TeleportService2.teleportTo(player, 210060000, 2456.86f, 2385.21f, 32.50000f, (byte) 32, TeleportAnimation.BEAM_ANIMATION);
				break;
				case 730172: //Dried-Out Vine.
				    TeleportService2.teleportTo(player, 210060000, 2842.82f, 2499.29f, 39.89f, (byte) 38, TeleportAnimation.BEAM_ANIMATION);
				break;
				case 730173: //Dried-Out Vine.
				    TeleportService2.teleportTo(player, 210060000, 2674.758f, 2947.456f, 37.47572f, (byte) 44, TeleportAnimation.BEAM_ANIMATION);
				break;
			}
        } else {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
		}
	}
}
