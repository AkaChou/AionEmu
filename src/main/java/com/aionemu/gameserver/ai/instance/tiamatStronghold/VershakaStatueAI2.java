package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Tiamat Stronghold 副本 NPC AI：Vershaka Statue（@AIName "vershakastatue"），继承 NpcAI2。
 * Tiamat Stronghold instance NPC AI: Vershaka Statue (@AIName "vershakastatue"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("vershakastatue")
public class VershakaStatueAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			GameEngineServices.skillEngine().applyEffectDirectly(300, player, player, 60000 * 1); //Transformation: Drakan.
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
