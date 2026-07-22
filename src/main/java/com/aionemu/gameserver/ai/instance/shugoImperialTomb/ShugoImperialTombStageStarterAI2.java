package com.aionemu.gameserver.ai.instance.shugoImperialTomb;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("shugo_imperial_tomb_stage_starter")
public class ShugoImperialTombStageStarterAI2 extends NpcAI2 {
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId != 10000) {
			return false;
		}
		Stage stage = switch (getNpcId()) {
			case 831110 -> new Stage("Condition_S2", 1401582);
			case 831111 -> new Stage("Condition_S3", 1401583);
			case 831112 -> new Stage("Condition_S4", 1401584);
			default -> throw new IllegalStateException("Unexpected Shugo Imperial Tomb stage starter: " + getNpcId());
		};
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), stage.variable(), 1, 0);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(stage.messageId()));
		AI2Actions.deleteOwner(this);
		return true;
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}

	private record Stage(String variable, int messageId) {
	}
}
