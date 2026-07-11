package com.aionemu.gameserver.ai.instance.bastionOfSouls;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Bastion Of Souls 副本 NPC AI：Koinus（@AIName "IDAb1_Ere_Sub_4F_Hole"），继承 NpcAI2。
 * Bastion Of Souls instance NPC AI: Koinus (@AIName "IDAb1_Ere_Sub_4F_Hole"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDAb1_Ere_Sub_4F_Hole")
public class KoinusAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 66) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			AI2Actions.deleteOwner(this);
		}
        return true;
    }
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(KoinusAI2.this);
			}
		}, 900000);
	}
}
