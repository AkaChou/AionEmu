package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(302330000)
public class KumukiCaveInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() != 703424) {
			return;
		}
		if (player.getInventory().decreaseByItemId(185000295, 1)) {
			npc.getController().onDelete();
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403686));
		}
	}

	private static void removePlayerState(Player player) {
		var inventory = player.getInventory();
		inventory.decreaseByItemId(186000459, inventory.getItemCountByItemId(186000459));
		removeEffects(player);
	}

	private static void removeEffects(Player player) {
		for (int effectId : new int[] { 16973, 16974, 17619, 17623 }) {
			player.getEffectController().removeEffect(effectId);
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removePlayerState(player);
	}
}
