package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Dragon Lord Refuge 副本 NPC AI：Kahrun（@AIName "kahrun2"），继承 NpcAI2。
 * Dragon Lord Refuge instance NPC AI: Kahrun (@AIName "kahrun2"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kahrun2")
public class KahrunAI2 extends NpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId != 10000) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		spawn(283154, getOwner().getX(), getOwner().getY(), getOwner().getZ(), getOwner().getHeading());
		AI2Actions.deleteOwner(this);
		return true;
	}
}
