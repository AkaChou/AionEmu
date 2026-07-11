package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Tiamat Stronghold 副本 NPC AI：Tahabata Statue（@AIName "tahabata_statue"），继承 NpcAI2。
 * Tiamat Stronghold instance NPC AI: Tahabata Statue (@AIName "tahabata_statue"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("tahabata_statue")
public class TahabataStatueAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352));
    }
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) {
			TeleportService2.teleportTo(player, 300510000, instanceId, 1109.2244f, 1053.5504f, 790.55963f, (byte) 52);
		}
		return true;
	}
}
