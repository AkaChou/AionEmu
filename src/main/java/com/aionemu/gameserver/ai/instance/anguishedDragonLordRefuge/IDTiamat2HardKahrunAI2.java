package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：ID Tiamat2 Hard Kahrun（@AIName "kahrun3"），继承 NpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: ID Tiamat2 Hard Kahrun (@AIName "kahrun3"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kahrun3")
public class IDTiamat2HardKahrunAI2 extends NpcAI2 {

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
