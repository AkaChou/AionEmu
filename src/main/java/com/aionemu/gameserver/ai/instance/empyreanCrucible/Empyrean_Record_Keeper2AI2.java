package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("record_keeper2")
public class Empyrean_Record_Keeper2AI2 extends NpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			set(player.getRace() == Race.ELYOS ? "condition_s1_l" : "condition_s1_d");
		} else if (dialogId == 10001) {
			set("stage7_start");
			for (Player member : getPosition().getWorldMapInstance().getPlayersInside()) {
				TeleportService2.teleportTo(member, getPosition().getWorldMapInstance().getMapId(),
						getPosition().getInstanceId(), 1793.9233f, 796.92f, 469.36542f, (byte) 60);
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		AI2Actions.deleteOwner(this);
		return true;
	}

	@Override
	protected void handleSpawned() {
		if (getNpcId() == 799567) {
			GameFeatureServices.npcShoutsService().sendMsg(
					getPosition().getWorldMapInstance(), 1111450, getObjectId(), false, 0, 2000);
		}
		super.handleSpawned();
	}

	private void set(String variable) {
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), variable, 0, 1);
	}
}
