package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 纪律训练场副本事件处理器。
 * Instance event handler for Discipline Training Grounds.
 *
 * @author Encom
 */

@InstanceID(300430000)
public class DisciplineTrainingGroundsInstance extends PvPArenaInstance
{
	/**
	 * 玩家采集完成时处理。
	 * Handle player gathering completion.
	 *
	 * 玩家 / player
	 * gatherable
	 */
	@Override
	public void onGather(Player player, Gatherable gatherable) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		getPlayerReward(player.getObjectId()).addPoints(1250);
		sendPacket();
		int nameId = gatherable.getObjectTemplate().getNameId();
		DescriptionId name = new DescriptionId(nameId * 2 + 1);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, name, 1250));
	}
}
