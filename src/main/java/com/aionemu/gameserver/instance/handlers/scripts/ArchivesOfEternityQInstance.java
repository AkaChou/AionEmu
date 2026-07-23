package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 永恒档案库任务副本事件处理器。
 * Instance event handler for Archives Of Eternity Q.
 *
 * @author Encom
 */

@InstanceID(301570000)
public class ArchivesOfEternityQInstance extends GeneralInstanceHandler {

	@Override
	public void onDie(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId != 857785 && npcId != 857792 && npcId != 857796 && npcId != 857800) {
			return;
		}
		Player player = npc.getAggroList().getMostPlayerDamage();
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, npcId == 857785 || npcId == 857796 ? 927 : 928));
		RetailConditionSpawnEngine.setVariable(npc.getPosition().getWorldMapInstance(), "SCENE", 13, 0);
		sendMsg(1403304, 0, false, 25, 0);
	}
}
