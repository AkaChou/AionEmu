package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(301550000)
public class CradleOfEternityInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() != 834007) {
			return;
		}
		if (player.getInventory().decreaseByItemId(185000267, 1)) {
			RetailConditionSpawnEngine.setVariable(instance, "ideternity_02_d_button", 2, 0);
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403448));
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}

	private void removeEffects(Player player) {
		player.getEffectController().removeEffect(21340);
		player.getEffectController().removeEffect(21344);
	}
}
