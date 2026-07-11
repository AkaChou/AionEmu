package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 位移/切换型 AI：触发位置或状态切换。
 * Shifter AI that triggers position or state transitions.
 *
 * @author Encom
 */
@AIName("shifter")
public class ShifterAI2 extends ActionItemNpcAI2
{
	/**
	 * 使用交互物完成时的逻辑。
	 * Logic when action-item use finishes.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleUseItemFinish(Player player) {
		super.handleUseItemFinish(player);
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(getOwner(), EmotionType.EMOTE, 144, 0), true);
	}
}
