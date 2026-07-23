package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

@InstanceID(320100000)
public class FireTempleInstance extends GeneralInstanceHandler {

	@Override
	public void onDie(Npc npc) {
		switch (npc.getNpcId()) {
			case 212846, 214621 -> spawnKromedeTreasureChest();
		}
	}

	private void spawnKromedeTreasureChest() {
		int chestId;
		int messageId;
		switch (Rnd.get(1, 3)) {
			case 1 -> { chestId = 833523; messageId = 1111313; }
			case 2 -> { chestId = 833524; messageId = 1111314; }
			default -> { chestId = 833525; messageId = 1111315; }
		}
		announceChest(messageId);
		spawn(chestId, 418.16385f, 95.81711f, 117.3052f, (byte) 50);
	}

	private void announceChest(int messageId) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(false, messageId, player.getObjectId(), 2));
				}
			}
		});
	}
}
