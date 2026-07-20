package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("cruciblerift")
public class CrucibleRiftAI2 extends ActionItemNpcAI2 {
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
			case 730459: //Crucible Rift.
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			break;
			case 730460: //Crucible Rift.
				TeleportService2.teleportTo(player, 300320000, getPosition().getInstanceId(), 1759.5004f, 1273.5414f, 389.11743f, (byte) 10);
				spawn(205679, 1765.522f, 1282.1051f, 389.11743f, (byte) 0);
				AI2Actions.deleteOwner(this);
			break;
		}
	}
	
	@Override
	protected void handleSpawned() {
		if (getNpcId() == 730459) {
			var instance = getPosition().getWorldMapInstance();
			GameFeatureServices.npcShoutsService().sendMsg(instance, 1111482, getObjectId(), false, 2, 2000);
			GameFeatureServices.npcShoutsService().sendMsg(instance, 1111483, getObjectId(), false, 2, 6000);
			GameFeatureServices.npcShoutsService().sendMsg(instance, 1111484, getObjectId(), false, 2, 10000);
		}
		super.handleSpawned();
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && getNpcId() == 730459) { //Crucible Rift.
			var instance = getPosition().getWorldMapInstance();
			RetailConditionSpawnEngine.setVariable(instance,
					player.getRace() == Race.ELYOS ? "hidden_L" : "hidden_D", 1, 0);
			RetailConditionSpawnEngine.setVariable(instance, "STAGE", player.getLevel() <= 50 ? 7 : 8, 0);
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
			TeleportService2.teleportTo(player, 300320000, getPosition().getInstanceId(), 1807.0531f, 306.2831f, 469.25f, (byte) 54);
			AI2Actions.deleteOwner(this);
		}
		return true;
	}
}
