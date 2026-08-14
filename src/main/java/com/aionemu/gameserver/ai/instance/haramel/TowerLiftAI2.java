package com.aionemu.gameserver.ai.instance.haramel;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Haramel 副本 NPC AI：Tower Lift（@AIName "tower_lift"），继承 NpcAI2。
 * Haramel instance NPC AI: Tower Lift (@AIName "tower_lift"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("tower_lift")
public class TowerLiftAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) {
			switch (player.getWorldId()) {
				case 300200000: //哈拉梅尔 2.0 / Haramel 2.0
					TeleportService2.teleportTo(player, 300200000, instanceId, 220.0f, 213.0f, 126.68472f, (byte) 0);
			    break;
				case 302330000: //库穆基洞穴 5.3 / Kumuki Cave 5.3
					TeleportService2.teleportTo(player, 302330000, instanceId, 220.0f, 213.0f, 126.68472f, (byte) 0);
			    break;
		    }
		}
		return true;
	}
}
