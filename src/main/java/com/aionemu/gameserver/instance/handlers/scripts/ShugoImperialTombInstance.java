package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(300560000)
public class ShugoImperialTombInstance extends GeneralInstanceHandler {
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21094);
		effectController.removeEffect(21103);
		effectController.removeEffect(21096);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}
}
