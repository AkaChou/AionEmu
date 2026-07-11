package com.aionemu.gameserver.ai.instance.shugoEmperorVault;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Shugo Emperor Vault 副本 NPC AI：Opened Vault Door（@AIName "opened_vault_door"），继承 NpcAI2。
 * Shugo Emperor Vault instance NPC AI: Opened Vault Door (@AIName "opened_vault_door"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("opened_vault_door")
public class OpenedVaultDoorAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 104) {
		    switch (getNpcId()) {
			    case 832924: //Opened Vault Door.
					switch (player.getWorldId()) {
					    case 301400000: //The Shugo Emperor's Vault.
					        TeleportService2.teleportTo(player, 301400000, instanceId, 426.50177f, 694.3207f, 398.42203f, (byte) 44);
						break;
						case 301590000: //Emperor Trillirunerk's Safe.
						    TeleportService2.teleportTo(player, 301590000, instanceId, 426.50177f, 694.3207f, 398.42203f, (byte) 44);
			            break;
					}
				break;
			}
		}
		return true;
	}
}
