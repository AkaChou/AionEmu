package com.aionemu.gameserver.ai.instance.shugoImperialTomb;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.ShugoImperialTombInstance;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("Crown_Prince_Admirer")
public class Crown_Prince_AdmirerAI2 extends NpcAI2 {
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000
				&& getPosition().getWorldMapInstance().getInstanceHandler() instanceof ShugoImperialTombInstance handler) {
			handler.startConditionStage("Condition_S2", 130);
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401582));
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
