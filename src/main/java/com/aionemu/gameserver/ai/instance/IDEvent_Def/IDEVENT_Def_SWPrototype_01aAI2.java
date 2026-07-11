package com.aionemu.gameserver.ai.instance.IDEvent_Def;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * ID Event Def 副本 NPC AI：IDEVENT Def SW Prototype 01a（@AIName "IDEVENT_Def_SWPrototype_01a"），继承 NpcAI2。
 * ID Event Def instance NPC AI: IDEVENT Def SW Prototype 01a (@AIName "IDEVENT_Def_SWPrototype_01a"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDEVENT_Def_SWPrototype_01a")
public class IDEVENT_Def_SWPrototype_01aAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(186000470, 1)) {
			spawn(836025, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
		} else if (dialogId == 10001 && player.getInventory().decreaseByItemId(186000470, 2)) {
			spawn(836030, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
		} else if (dialogId == 10002 && player.getInventory().decreaseByItemId(186000470, 3)) {
			spawn(836035, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
		} else if (dialogId == 10003 && player.getInventory().decreaseByItemId(186000470, 3)) {
			spawn(836036, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
		} else if (dialogId == 10004 && player.getInventory().decreaseByItemId(186000470, 5)) {
			spawn(836045, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
}
