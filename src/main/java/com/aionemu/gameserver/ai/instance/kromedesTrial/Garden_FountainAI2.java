package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Garden Fountain（@AIName "garden_fountain"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Garden Fountain (@AIName "garden_fountain"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("garden_fountain")
public class Garden_FountainAI2 extends NpcAI2
{
    @Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
        if (dialogId == 1012) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400655));
			GameEngineServices.skillEngine().getSkill(getOwner(), 19216, 1, player).useNoAnimationSkill();
        }
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
}
