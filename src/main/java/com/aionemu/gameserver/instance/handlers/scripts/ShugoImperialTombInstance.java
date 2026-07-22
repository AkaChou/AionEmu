package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(300560000)
public class ShugoImperialTombInstance extends GeneralInstanceHandler {
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() == 831095) {
			GameEngineServices.skillEngine().getSkill(npc, 21096, 60, player).useNoAnimationSkill();
		}
	}

	public void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182006989, storage.getItemCountByItemId(182006989));
		storage.decreaseByItemId(182006990, storage.getItemCountByItemId(182006990));
		storage.decreaseByItemId(182006991, storage.getItemCountByItemId(182006991));
		storage.decreaseByItemId(182006999, storage.getItemCountByItemId(182006999));
	}

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21096);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}
}
