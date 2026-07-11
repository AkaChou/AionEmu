package com.aionemu.gameserver.ai.instance.aturamSkyFortress;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Aturam Sky Fortress 副本 NPC AI：Lift Control（@AIName "Lift_Control"），继承 NpcAI2。
 * Aturam Sky Fortress instance NPC AI: Lift Control (@AIName "Lift_Control"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Lift_Control")
public class Lift_ControlAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		switch (getNpcId()) {
		    case 730538: //Lift Control.
		        switch (player.getWorldId()) {
		            case 300240000: //Aturam Sky Fortress.
				        if (dialogId == 10000) {
							TeleportService2.teleportTo(player, 300240000, instanceId, 691.5343f, 457.017f, 655.5343f, (byte) 0);
					    }
				    break;
			    } switch (player.getWorldId()) {
		            case 300241000: //[Event] Aturam Sky Fortress.
				        if (dialogId == 10000) {
							TeleportService2.teleportTo(player, 300241000, instanceId, 691.5343f, 457.017f, 655.5343f, (byte) 0);
					    }
				    break;
			    }
			break;
		}
		return true;
	}
}
