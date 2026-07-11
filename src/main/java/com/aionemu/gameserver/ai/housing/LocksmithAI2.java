package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 房屋相关 NPC AI：Locksmith（@AIName "locksmith"），继承 NpcAI2。
 * Housing-related NPC AI: Locksmith (@AIName "locksmith"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("locksmith")
public class LocksmithAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(182006830) != null ||
		    player.getInventory().getFirstItemByItemId(182006831) != null ||
			player.getInventory().getFirstItemByItemId(182006832) != null ||
			player.getInventory().getFirstItemByItemId(182006833) != null ||
			player.getInventory().getFirstItemByItemId(182006834) != null ||
			player.getInventory().getFirstItemByItemId(182006835) != null ||
			player.getInventory().getFirstItemByItemId(182006836) != null ||
			player.getInventory().getFirstItemByItemId(182006837) != null ||
			player.getInventory().getFirstItemByItemId(182006838) != null) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
		    // 锁匠。 / Locksmith.
			switch (getNpcId()) {
		        case 810011:
				case 810012:
				    // 需找方法，因为两个 NPC 都开“上锁箱”，玩家无法单独打开。 / Need find a way, coz is this both npc open "Lockedbox", no player alone.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
